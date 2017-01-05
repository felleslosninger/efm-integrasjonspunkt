$(function() {
    var proxyUrl = "http://nexusproxy.azurewebsites.net/latest?env=staging&callback=?";
    $.getJSON( proxyUrl)
        .done(function( data ) { 
        $( "#downloadurl" ).attr("data-url", data.downloadUri);
        $( "#titleField").text(data.baseVersion);
        $( "#subtitle1").text( " Sha1: "+ data.sha1);
        $( "#subtitle2").text("Trykk her for å laste ned siste versjon");
        })
});