# File included in the Docker Makefiles.

# Variables:
# IMAGE_NAME        The name of the Docker image
# IMAGE_TAG         The tag of the Docker image
# REGISTRY          The hostname and port of the registry; or empty
# REGISTRY_PATH     The path in the registry, ending with a slash; or empty
# REGISTRY_USERNAME The username for logging into the registry
# REGISTRY_PASSWORD The password for logging into the registry
# NAME              The container name
# CMD               The command to execute
# RUN_ARGS          The container run arguments
# EXEC_ARGS         The container exec arguments
# SRC               The file or folder to copy from the host
# DEST              The path to copy to in the container

# NOTE: By default the containers are removed when stopped
# (through the use of the --rm flag).  To prevent this, redefine RUN_ARGS="":
#   make run RUN_ARGS=""

IMAGE_TAG         ?= latest
REGISTRY          ?= gitlab.ewi.tudelft.nl:4242
REGISTRY_USERNAME ?=
REGISTRY_PASSWORD ?=
REGISTRY_PATH     ?= /spoofax/docker-images/
REGISTRY_URL      ?= $(REGISTRY)$(REGISTRY_PATH)
NAME              := $(subst /,-,tmp-$(IMAGE_NAME))
CMD               := /bin/bash
RUN_ARGS          := --rm
EXEC_ARGS         :=
FILENAME          := $(notdir $(IMAGE_NAME)).tar

# Asserts that a variable is defined:
check_defined = \
    $(strip $(foreach 1,$1, \
        $(call __check_defined,$1,$(strip $(value 2)))))
__check_defined = \
    $(if $(value $1),, \
        $(error Undefined $1$(if $2, ($2))$(if $(value @), \
                required by target `$@')))

.PHONY: all build clean run start attach exec stop put get login deploy save load

.SILENT:

all: build


# Builds the Docker image in the current Docker machine
# Usage:
#     make                          Builds the docker image
#     make IMAGE_TAG="3.0"          Builds the docker image and assigns the specified tag
build:
	@:$(call check_defined, IMAGE_NAME, image name)
	@:$(call check_defined, IMAGE_TAG, image tag)
	docker image build \
	  --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
	  --build-arg VCS_REF=$(git rev-parse --short HEAD) \
	  -t $(IMAGE_NAME):$(IMAGE_TAG) .
	docker image tag $(IMAGE_NAME):$(IMAGE_TAG) $(IMAGE_NAME):latest


# Deletes the Docker image from the current Docker machine
# Usage:
#     make clean
clean:
	@:$(call check_defined, IMAGE_NAME, image name)
	@:$(call check_defined, IMAGE_TAG, image tag)
	-docker image rm $(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null
	-docker image rm $(IMAGE_NAME):latest 2>/dev/null
	-docker image rm $(REGISTRY_URL)$(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null
	-docker image rm $(REGISTRY_URL)$(IMAGE_NAME):latest 2>/dev/null


# Starts the Docker container from the image and executes the specified command
# Usage:
#     make run
#     make run CMD=/bin/sh
run:
	@:$(call check_defined, IMAGE_NAME, image name)
	@:$(call check_defined, IMAGE_TAG, image tag)
	@:$(call check_defined, NAME, container name)
	@:$(call check_defined, CMD, command)
	docker container run -it $(RUN_ARGS) --name $(NAME) $(IMAGE_NAME):$(IMAGE_TAG) $(CMD)


# Starts the detached Docker container from the image
# Usage:
#     make start                    Starts the container with sh
#     make start NAME=mycontainer   Assign a name to the container
#     make start CMD=/bin/sh        Starts the container with a custom command
#     make start CMD=               Starts the container with the CMD in the Dockerfile
start:
	@:$(call check_defined, IMAGE_NAME, image name)
	@:$(call check_defined, IMAGE_TAG, image tag)
	@:$(call check_defined, NAME, container name)
	docker container run -itd $(RUN_ARGS) --name $(NAME) $(IMAGE_NAME):$(IMAGE_TAG) $(CMD)


# Attach to a running container
# Usage:
#     make attach
#     make attach NAME=mycontainer
attach:
	@:$(call check_defined, NAME, container name)
	docker container attach $(NAME)


# Executes a command in a running container
# Usage:
#     make exec CMD="ls -la"        Executes a custom command in the container
#     make exec NAME=mycontainer    Executes a command in the container with the specified name
exec:
	@:$(call check_defined, IMAGE_NAME, image name)
	@:$(call check_defined, IMAGE_TAG, image tag)
	@:$(call check_defined, CMD, command)
	docker container exec -it $(EXEC_ARGS) $(NAME) $(CMD)


# Stops a running container
# Usage:
#     make stop                     Stops a running container
#     make stop NAME=mycontainer    Stops the container with the specified name
stop:
	@:$(call check_defined, NAME, container name)
	docker container stop $(NAME)


# Copies a file into a running container
# Usage:
#     make put SRC=myfile.txt DEST=/myfolder/myfile.txt     Copies a local file or folder to the specified path in the container
put:
	@:$(call check_defined, NAME, container name)
	@:$(call check_defined, SRC, source)
	@:$(call check_defined, DEST, destination)
	docker container cp $(SRC) $(NAME):$(DEST)


# Copies a file out of a running container
# Usage:
#     make get SRC=/myfolder/myfile.txt DEST=myfile.txt     Copies a file or folder from the container to the specified path
get:
	@:$(call check_defined, NAME, container name)
	@:$(call check_defined, SRC, source)
	@:$(call check_defined, DEST, destination)
	docker container cp $(NAME):$(SRC) $(DEST)


# Logs into the Docker image registry
# Usage:
#     make login                   Logs into the default registry
#     make login REGISTRY="localhost:4242"   Logs into the specified registry
login:
	docker login $(REGISTRY) \
	$(if $(REGISTRY_USERNAME), --username $(REGISTRY_USERNAME)) \
	$(if $(REGISTRY_PASSWORD), --password $(REGISTRY_PASSWORD))


# Pushes the image to the remote registry
# Usage:
#     make deploy                   Pushes the docker image to the default registry
#     make deploy REGISTRY="localhost:4242"   Pushes the docker image to the specified registry
deploy:
	@:$(call check_defined, IMAGE_NAME, image name)
	@:$(call check_defined, IMAGE_TAG, image tag)
	docker image tag $(IMAGE_NAME):$(IMAGE_TAG) $(REGISTRY_URL)$(IMAGE_NAME):$(IMAGE_TAG)
	docker image tag $(IMAGE_NAME):$(IMAGE_TAG) $(REGISTRY_URL)$(IMAGE_NAME):latest
	docker image push $(REGISTRY_URL)$(IMAGE_NAME):$(IMAGE_TAG)
	docker image push $(REGISTRY_URL)$(IMAGE_NAME):latest
	-docker image rm $(REGISTRY_URL)$(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null
	-docker image rm $(REGISTRY_URL)$(IMAGE_NAME):latest 2>/dev/null


# Save the Docker image to a TAR file
# Usage:
#     make save                          Saves the docker image to a TAR file
#     make save FILENAME="output.tar"    Saves the docker image to the specified filename
save:
	@:$(call check_defined, IMAGE_NAME, image name)
	@:$(call check_defined, IMAGE_TAG, image tag)
	@:$(call check_defined, FILENAME, output filename)
	docker save -o $(FILENAME) $(IMAGE_NAME):$(IMAGE_TAG)


# Loads the Docker image from a TAR file
# Usage:
#     make load                          Loads the docker image from a TAR file
#     make load FILENAME="output.tar"    Loads the docker image from the specified filename
load:
	@:$(call check_defined, FILENAME, input filename)
	docker load -i $(FILENAME)

