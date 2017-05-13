<html>
<header>
	<!-- helper libraries -->
	<script src="resources/js/jquery-1.12.2.min.js" type="text/javascript"></script>

	<!-- daypilot libraries -->
	<script src="resources/js/daypilot-all.min.js?v=2848"
		type="text/javascript"></script>

	<!-- daypilot themes -->
	<link type="text/css" rel="stylesheet"
		href="resources/css/areas.css?v=2848" />

	<link type="text/css" rel="stylesheet"
		href="resources/css/month_white.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/month_green.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/month_transparent.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/month_traditional.css?v=2848" />

	<link type="text/css" rel="stylesheet"
		href="resources/css/navigator_8.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/navigator_white.css?v=2848" />

	<link type="text/css" rel="stylesheet"
		href="resources/css/calendar_transparent.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/calendar_white.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/calendar_green.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/calendar_traditional.css?v=2848" />

	<link type="text/css" rel="stylesheet"
		href="resources/css/scheduler_8.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/scheduler_white.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/scheduler_green.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/scheduler_blue.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/scheduler_traditional.css?v=2848" />
	<link type="text/css" rel="stylesheet"
		href="resources/css/scheduler_transparent.css?v=2848" />

	<script type="text/javascript">
	
		function convertDayOfWeek(dayOfWeek) {
			
			var result;
			
			switch(dayOfWeek) {
			case 0:
				result=1;
				break;
			case 1:
				result=2;
				break;
			case 2:
				result=3;
				break;
			case 3:
				result=4;
				break;
			case 4:
				result=5;
				break;
			case 5:
				result=6;
				break;
			case 6:
				result=7;
				break;
			}
			
			return result;
		}
	
		$(document).ready(
				function() {

					var dp = new DayPilot.Scheduler("dp");
					dp.startDate = DayPilot.Date.today().firstDayOfWeek()
							.addDays(1);
					//new DayPilot.Date("2013-03-25").firstDayOfMonth();
					dp.locale = "fr-fr";
					//dp.weekStarts = 1; //Monday
					dp.scale = "CellDuration";
					dp.cellDuration = 30;
					dp.days = 7;
					dp.allowEventOverlap = false;
					//dp.heightSpec = "Parent100Pct";

					dp.timeHeaders = [ {
						groupBy : "Day",
						format : "dddd"
					}, {
						groupBy : "Hour"
					} ];

					// event creating
					dp.onTimeRangeSelected = function(args) {
						var name = prompt("Indiquer la température:", "");
						if (!name) {
							dp.clearSelection();
							return;
						}
						
						var ev = {				                 
				                 "start": args.start.toString(),
				                 "end": args.end.toString(),
				                 "resource": args.resource,
								 "text": name,
								 "dayofweek": convertDayOfWeek(args.start.getDayOfWeek())
				             };		 
						
						$.ajax({ 
				             type: "POST",
				             dataType:"json",
				             contentType: "application/json; charset=utf-8",
				             url: "http://localhost:8080/automationserver-webapp/rest/schedule/create",
				             data: JSON.stringify(ev),
				             success: function(data) {
				            	 var e = new DayPilot.Event({
					                  start: ev.start,
					                  end: ev.end,
					                  id: data.id,
					                  text: ev.text,
					                  dayofweek: ev.dayofweek,
					                  resource: ev.resource
					              });
				            	 
				            	    dp.events.add(e);				            	   
									dp.clearSelection();
									//dp.message("Créé!");	 
				             }
				             
				             
				         });
					};
					
				
					dp.onEventResized = function(args) {
												
						var ev = {
				                 "id": args.e.id(),
				                 "start": args.newStart.toString(),
				                 "end": args.newEnd.toString(),
				                 "resource": args.e.resource(),
								 "text": args.e.text(),
								 "dayofweek": args.e.data.dayofweek
				             };		 
						
						$.ajax({ 
				             type: "PUT",
				             dataType:"json",
				             contentType: "application/json; charset=utf-8",
				             url: "http://localhost:8080/automationserver-webapp/rest/schedule/update",
				             data: JSON.stringify(ev)
				             
				         });
					};

					dp.contextMenu = new DayPilot.Menu({
						items : [
								{
									text : "Afficher l'ID",
									onclick : function() {
										alert("Paramétrage: "
												+ this.source.value());
									}
								},
								{
									text : "Editer",
									onclick : function() {
										
										var name = prompt("Nouvelle températures:", evt.text());
									    if (!name) return;
									    	
									    	var evt = dp.events.find(this.source.id());
									    	evt.text(name);
									    
									    	var ev = {
									                 "id": evt.id(),
									                 "start": evt.start(),
									                 "end": evt.end(),
									                 "resource": evt.resource(),
													 "text": evt.text(),
													 "dayofweek": evt.data.dayofweek
									             };		 
											
											$.ajax({ 
									             type: "PUT",
									             dataType:"json",
									             contentType: "application/json; charset=utf-8",
									             url: "http://localhost:8080/automationserver-webapp/rest/schedule/update",
									             data: JSON.stringify(ev)
									             
									         });
											
											dp.events.update(evt);
																		
									}
								},
								{
									text : "Supprimer",
									onclick : function() {
										
										if (!confirm("Voulez vous vraiment supprimer ce paramétrage?")) {
										      return;
										    }
										
										$.ajax({ 
								             type: "DELETE",
								             datatype: "json",
								             url: "http://localhost:8080/automationserver-webapp/rest/schedule/delete/" + this.source.value(),								             
								         });
										
										dp.events.remove(this.source);
						            	//dp.update();
						            	//dp.message("Supprimé!");
									}
								} ]
					});

					dp.resources = [ {
						name : "Bureau",
						id : 1
					}, {
						name : "Salon",
						id : 2
					}, {
						name : "Chambre Manon",
						id : 4
					}, {
						name : "Chambre Nohé",
						id : 3
					}, {
						name : "Chambre Parents",
						id : 5
					} ];

					dp.init();

					dp.onBeforeEventRender = function(args) {
						args.e.moveVDisabled = true;
					};

					dp.events.load("http://localhost:8080/automationserver-webapp/rest/schedule");
				});
	</script>
</header>
<body>
	<h2>Paramétrage de la tempature par défaut des pièces</h2>
		<div id="dp"></div>
</body>
</html>

