FNR==1 {
  # printf("\nstart %s: ", FILENAME) > "/dev/stderr"
  concrete=0;
}
{
  output=$0;
  # If not constructor definition
  if(!($0 ~ /^\s*(Simple|InverseReferenceOnlyAnno|ReadOnlyPage|WebService|AllEntityPropertiesCopy|TemplateCallPropertyNoLoadingFeedback|ImmutableReference|AnyProp|Null)\s*:/)\
     && !($0 ~ /^\s*signature constructors (Simple|InverseReferenceOnlyAnno|ReadOnlyPage|WebService|AllEntityPropertiesCopy|TemplateCallPropertyNoLoadingFeedback|ImmutableReference|AnyProp|Null)\s*:/)) {
    # Replace nullary constructors with the same followed by empty brackets
    output=gensub(/\<(Simple)\>([^("])/,"\\1()\\2","g",output)
    output=gensub(/\<(InverseReferenceOnlyAnno)\>([^("])/,"\\1()\\2","g",output)
    output=gensub(/\<(ReadOnlyPage)\>([^("])/,"\\1()\\2","g",output)
    output=gensub(/\<(WebService)\>([^("])/,"\\1()\\2","g",output)
    output=gensub(/\<(AllEntityPropertiesCopy)\>([^("])/,"\\1()\\2","g",output)
    output=gensub(/\<(TemplateCallPropertyNoLoadingFeedback)\>([^("])/,"\\1()\\2","g",output)
    output=gensub(/\<(ImmutableReference)\>([^("])/,"\\1()\\2","g",output)
    output=gensub(/\<(AnyProp)\>([^("])/,"\\1()\\2","g",output)
    output=gensub(/\<(Null)\>([^("])/,"\\1()\\2","g",output)
  }
  # If the start of concrete syntax
  if($0 ~ /\|\[/) {
    # And not strategy call with immediate term argument which is a list
    if(!($0 ~ /\(\|\[/)) {
      # Note that concrete syntax may be nested through anti-quoting to Stratego
      concrete+=1;
      # printf("%d", concrete) > "/dev/stderr"
    }
  }
  # If inside concrete syntax
  if(concrete > 0) {
    # And not the overlay definition
    if(!($0 ~ /e_HibSession =/ || $0 ~ /e_UTILS =/)) {
      # Replace nullary overlay reference with expression using the overlay name with empty brackets
      output=gensub(/e_HibSession/,"~e:(e_HibSession())","g",output)
      output=gensub(/e_UTILS/,"~e:(e_UTILS())","g",output)
    }
    # If end of concrete syntax
    if($0 ~ /\]\|/) {
      # Note that concrete syntax may be nested
      concrete-=1;
      # printf("%d", concrete) > "/dev/stderr"
    }
  } else {
    # If not inside concrete syntax, replace nullary overlay as normal with same + empty brackets
    output=gensub(/\<(e_HibSession)\>([^("])/,"\\1()\\2","g",output)
    output=gensub(/\<(e_UTILS)\>([^("])/,"\\1()\\2","g",output)
  }
  print output
}