if (!window.lcoiapi) {
  lcoiapi = {};
}
lcoiapi.classificationList = {
  "AEISSUE": "Unexpected unacceptable side effects",
  "COMPLETED": "More accurately described as completed",
  "DESCRIPTION": "The reason for stopping may be in the description",
  "DESIGNCHANGE": "The trial was stopped due to changes in the study design",
  "EXTERNAL": "The trial was stopped due to external factors",
  "FUNDING": "Insufficient funding",
  "HIGHBENEFIT": "Unequivocal evidence of treatment benefit",
  "LOWCOMPLIANCE": "Lack of compliance in a large number of patients",
  "LOWENROLLMENT": "Failure to include enough patients at a sufficient rate",
  "PERSONNEL": "Personnel issues, like Principal Investigator change",
  "SAEISSUE": "Unequivocal evidence of treatment harm",
  "STRATEGIC": "Business reasons",
  "UNKNOWN": "Unable to determine a valid classification",
  "UNLIKELYBENEFIT": "No emerging trends and no reasonable chance of benefit"
};
lcoiapi.Selector = {};
lcoiapi.annotations = [];
lcoiapi.annotationUrls = [];

lcoiapi.Selector.getSelected = function() {
  var text = '';
  if (window.getSelection) {
    text = window.getSelection();
  } else if (document.getSelection) {
    text = document.getSelection();
  } else if (document.selection) {
    text = document.selection.createRange().text;
  }
  return text;
}
lcoiapi.Selector.mouseupGoogle = function() {
  var text = lcoiapi.Selector.getSelected();
  if (text != '') {
    var searchLabel = "Search for '"+text+"'";
    $("div#classify_dialog").attr("title", "Classification NOT based on 'Why Stopped' field:")
    var classForm = $("<form>").attr("id", "classForm");
    var stoppedClass = $("<select>").attr("id", "stoppedClass");
    for (var key in lcoiapi.classificationList) {
      stoppedClass.append($("<option>").attr("value", key).html(key+": "+lcoiapi.classificationList[key]));
    }
    classForm.append($("<h3>").html('Search for: \"'+text+'\"'));
    classForm.append(stoppedClass);
    classForm.append($("<input>").attr("type","hidden").attr("id","selectedText").attr("value",text));
    classForm.append($("<p>").html("If you find a relevant URL that provides some insight into why you would select the  particular classification, please copy the URL into this field."));
    classForm.append($("<input>").attr("type", "text").attr("id","relevantUrl"));
    $('#classify_dialog').html(classForm);
    $('#classify_dialog').dialog({
      autoOpen: true, modal: true,
      height: 600, width: 800,
      buttons: {
        "Google Search": function() {
          window.open("http://www.google.com/search?q="+$("#selectedText").val())
        },
        "Annotate with URL": function() {
          var selectedClass = $("select#stoppedClass option:selected").val();
          var relevantUrl = $("#relevantUrl").val();
          $("div#annotationUrlClass").append($("<p>").html(selectedClass));
          $("div#annotationUrlText").append($("<p>").html(relevantUrl));
          lcoiapi.annotationUrls.push([selectedClass, relevantUrl]);
          $("#annotationJson").val(JSON.stringify(lcoiapi.annotations));
          $("#annotationUrlJson").val(JSON.stringify(lcoiapi.annotationUrls));
          $("div#annotationz").show();
          $(this).dialog("close");
        },
        "Cancel": function() {
          $(this).dialog("close");
        }
      }
    });
  }
}

lcoiapi.Selector.mouseupWhyStopped = function() {
  var text = lcoiapi.Selector.getSelected();
  if (text != '') {
    $("div#classify_dialog").attr("title", "Selected text to classify:")
    var classForm = $("<form>").attr("id", "classForm");
    var stoppedClass = $("<select>").attr("id", "stoppedClass");
    for (var key in lcoiapi.classificationList) {
      stoppedClass.append($("<option>").attr("value", key).html(key+": "+lcoiapi.classificationList[key]));
    }
    classForm.append($("<h3>").html('\"'+text+'\"'));
    classForm.append(stoppedClass);
    classForm.append($("<input>").attr("type","hidden").attr("id","selectedText").attr("value",text));
    $('#classify_dialog').html(classForm);
    $('#classify_dialog').dialog({
      autoOpen: true, modal: true,
      height: 300, width: 600,
      buttons: {
        "Classify this text": function() {
          var selectedClass = $("select#stoppedClass option:selected").val();
          var selectedText = $("#selectedText").val();
          $("div#annotationClass").append($("<p>").html(selectedClass));
          $("div#annotationText").append($("<p>").html(selectedText));
          lcoiapi.annotations.push([selectedClass, selectedText]);
          $("#annotationJson").val(JSON.stringify(lcoiapi.annotations));
          $("#annotationUrlJson").val(JSON.stringify(lcoiapi.annotationUrls));
          $("div#annotationz").show();
          $(this).dialog("close");
        },
        "Cancel": function() {
          $(this).dialog("close");
        }
      }
    });
    
  }
}

function classifyLoaded() {
  $("div#accordion").accordion({autoHeight:false,active:1,collapsible:true});
  $("p.hiliteAnnotation").bind("mouseup", lcoiapi.Selector.mouseupGoogle);
  $("span#why_stopped").bind("mouseup", lcoiapi.Selector.mouseupWhyStopped);
  $("#submitAnnotation, #cancelSubmission").button();
  $("#cancelSubmission").click(function() {
    $("div#annotationClass").html("<h5>Category</h5>");
    $("div#annotationText").html("<h5>Selected Text</h5>");
    $("div#annotationUrlClass").html("<h5>Category</h5>");
    $("div#annotationUrlText").html("<h5>Relevant Url</h5>");
    lcoiapi.annotations = [];
    lcoiapi.annotationUrls = [];
    $("#annotationJson").val(JSON.stringify(lcoiapi.annotations));
    $("#annotationUrlJson").val(JSON.stringify(lcoiapi.annotationUrls));
    $("div#annotationz").hide();
  })
}

