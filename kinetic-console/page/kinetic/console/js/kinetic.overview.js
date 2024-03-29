Kinetic.Portal.prototype.renderRack = function (rack) {
    $("#overview").empty();
    $("#detail").empty();
    var self = this;
    var totalDrives = 0;
    var normalDrives = 0;

    // render the rack overview chart
    var chassisArray = rack.listChassises();
    var driveArray;
    $("#overview").append("<div id=rackContainer class=rack></div>");
    var chassisIndex;
    var driveIndex;
    var stateIndex;
    var currentState = Kinetic.State.NORMAL;
    for (chassisIndex = chassisArray.length - 1; chassisIndex >= 0; chassisIndex--) {
        var cid = "chassis" + chassisArray[chassisIndex].id;
        var chassisDiv = "<div id=" + cid + " class=chassis></div>";
        $("#rackContainer").append(chassisDiv);

        driveArray = chassisArray[chassisIndex].listDrives();
        for (driveIndex = 0; driveIndex < driveArray.length; driveIndex++) {
            ++totalDrives;
            var drivehtml = "<img class='drive_img' ";
            drivehtml += " id='" + driveArray[driveIndex].wwn + "'";

            if (currentState == Kinetic.State.NORMAL) {
                ++normalDrives;
                drivehtml += " src=img/drive_green_1.png />";
            } else if (currentState == Kinetic.State.UNREACHABLE) {
                drivehtml += " src=img/drive_yellow_1.png />";
            } else {
                drivehtml += " src=img/drive_red_1.png />";
            }
            $("#" + cid).append(drivehtml);

            if (driveIndex == 2) {
                var mgmtNicHtml = "<img class='mgmt_nic_img' ";
                mgmtNicHtml += " id='" + rack.location + "_mgmtNic_" + chassisArray[chassisIndex].id + "'";
                mgmtNicHtml += " src=img/nic4.png />";
                $("#" + cid).append(mgmtNicHtml);
            }

            if (driveIndex == 5 || driveIndex == 8) {
                $("#" + cid).append("<img class=data_nic_img src=img/nic4.png />");
            }

            $('#' + rack.location + "_mgmtNic_" + chassisArray[chassisIndex].id).tooltipster({
                content: $('<samp>Mgmt IP: ' + chassisArray[chassisIndex].mgtIp1 + '</samp></br>')
            });

            $('#' + driveArray[driveIndex].wwn).tooltipster({
                content: $('<samp>WWN: ' + driveArray[driveIndex].wwn + '</samp></br>'
                    + '<samp>IP1: ' + driveArray[driveIndex].ip1 + '</samp></br>'
                    + '<samp>IP2: ' + driveArray[driveIndex].ip2 + '</samp></br>'
                    + '<samp>UNIT: ' + chassisArray[chassisIndex].unit + '</samp></br>')
            });
        }
    }

    // show the table abstraction

    // rack abstraction
    $("#detail").append("<h3></h3>");
    $("#detail").append("<table id='rack_abstract' class='table table-bordered'></table>");

    var theadContent = "";
    theadContent += "<thead><tr class='info'>";
    theadContent += "<th><strong>Rack Location</strong></th>";
    theadContent += "<th><strong>Chassis</strong></th>";
    theadContent += "<th><strong>Drives (normal/total)</strong></th>";
    theadContent += "<th><strong>Capacity (free/total)</strong></th>";
    theadContent += "</tr></thead>";
    $("#rack_abstract").append(theadContent);

    var tableContent = "";
    tableContent += "<tbody>";
    tableContent += "<tr class='warning'>";
    tableContent += "<td>" + rack.location + "</td>";
    tableContent += "<td>" + chassisArray.length + "</td>";
    tableContent += "<td id='drive_state_stats'>" + normalDrives + "/" + totalDrives + "</td>";
    tableContent += "<td id='drive_cap_stats'>" + "-/-" + " (GB)" + "</td>";
    tableContent += "</tr>";
    tableContent += "</tbody>";
    $("#rack_abstract").append(tableContent);

    // chassis abstraction
    $("#detail").append("<h3></h3>");
    $("#detail").append("<table id='chasssis_abstract' class='table table-bordered'></table>");

    theadContent = "";
    theadContent += "<thead><tr class='info'>";
    theadContent += "<th><strong>Unit</strong></th>";
    theadContent += "<th><strong>Manage IP</strong></th>";
    theadContent += "<th><strong>Drives</strong></th>";
    theadContent += "</tr></thead>";
    $("#chasssis_abstract").append(theadContent);


    var driveList;
    tableContent = "";
    tableContent += "<tbody>";
    for (i = chassisArray.length - 1; i >= 0; i--) {
        tableContent += "<tr class='warning'>";
        tableContent += "<td>" + chassisArray[i].unit + "</td>";
        tableContent += "<td>" + chassisArray[i].mgtIp1 + "</td>";
        tableContent += "<td>";
        driveList = chassisArray[i].listDrives();
        tableContent += "<strong>[Plane A]</strong> ";
        for (j = 0; j < driveList.length; j++) {
            tableContent += driveList[j].ip1;
            tableContent += ":";
            tableContent += driveList[j].port;
            tableContent += "; "
        }

        tableContent += "<br/><strong>[Plane B]</strong> ";
        for (j = 0; j < driveList.length; j++) {
            tableContent += driveList[j].ip2;
            tableContent += ":";
            tableContent += driveList[j].port;
            tableContent += "; "
        }
        tableContent += "</td>";
        tableContent += "</tr>";
    }
    tableContent += "</tbody>";
    $("#chasssis_abstract").append(tableContent);

    refreshChartsAndTables();

};

$(document).ready(function () {
	$.getJSON(Kinetic.Config.URL_DESCRIBE_ALL_DEVICES, function (json) {
		driveStates = json;
		
		portal = new Kinetic.Portal();
	    portal.loadRackList();

	    $("#racks_dropbox").change(function () {
	        var selectedRack = $("#racks_dropbox option:selected").text();

	        var racks = portal.racks;
	        var index;
	        for (index = 0; index < racks.length; index++) {
	            if (selectedRack == racks[index].location) {
	                portal.renderRack(racks[index]);
	            }
	        }
	    });

	    setInterval(function() {
	    	$.getJSON(Kinetic.Config.URL_DESCRIBE_ALL_DEVICES, function (json) {
	    		driveStates = json;
                refreshChartsAndTables();
	    	});
	    }, Kinetic.Config.CHARTS_REFRESH_PERIOD_IN_SEC * 1000);
	});
	
	
    setInterval(function() {
		if ($("#racks_dropbox").children().length <= 0)
		{
	        window.location.reload();
		}
	},2000);
	
});
