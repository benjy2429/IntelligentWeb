$(window).load(function() {

	
	// Prevent forms from performing default action
	$("form").on("submit", function(e) {
		e.preventDefault();
	});
	
	
	$("#form1Submit").click(function() {
		$("#dynamicText").html("Loading...");
		$.ajax({
			url: 'Servlet',
			type: 'post',
			datatype: 'json',
			data: $('#form1').serialize(),
			success: function(data){
				var json = data.split("\n");
				var tweetIds = JSON.parse(json[0]);
				var tweets = JSON.parse(json[1]);

				var result = "";
				var i = 0;
				$.each( tweets , function() {
					result += "<div class='tweet'>";
					result += "<form class='retweetersForm'><input type='hidden' name='formId' value='retweetersForm'>";
					result += "<input type='hidden' class='tweetId' name='tweetId' value='" + tweetIds[i] + "'></form>";
					result += "<img class='tweetImg' src='" + this.user.profileImageUrl + "'/>";
					result += "<div class='tweetContent'>";
					result += "<div class='tweetUser'>" + this.user.name + " (@<span class='tweetScreenName'>" + this.user.screenName + "</span>)</div>";
					result += "<div class='tweetText'>" + this.text + "</div>";
					result += "<a href='' class='retweets'>See who retweeted this</a>";
					result += "</div>";
					result += "<div id='retweetsFor" + tweetIds[i] + "'></div>";
					result += "</div>";
					i++;
				});
				$("#dynamicText").html(result);
				
				/*
				$(".tweet").click(function() {
					alert( this.find(".tweetScreenName").text() );
				});
				*/
			},
			error: function(xhr,textStatus,errorThrown){
				$("#dynamicText").html(errorThrown);
			}
		});
		
	});
	
	
	$("#form2Submit").click(function() {
		$("#dynamicText").html("Loading...");
		$.ajax({
			url: 'Servlet',
			type: 'post',
			datatype: 'json',
			data: $('#form2').serialize(),
			success: function(data){
				$("#dynamicText").html( JSON.parse(data) );
			},
			error: function(jqXHR,textStatus,errorThrown){
				$("#dynamicText").html(errorThrown);
			}
		});
	});
	
	
	$("#form3Submit").click(function() {
		$("#dynamicText").html("Loading...");
		$.ajax({
			url: 'Servlet',
			type: 'post',
			datatype: 'json',
			data: $('#form3').serialize(),
			success: function(data){
				var json = data.split("\n");
				var result = "";
				
				// Twitter user info
				var user = JSON.parse(json[0]);
				result += "<div class='tweet'>";
				result += "<img src='" + user.profileImageUrl + "' class='tweetImg' />";
				result += "<div class='tweetContent'><div class='tweetUser'>" + user.name + "</div>(@" + user.screenName + ")</div>";
				result += "</div>";
				
				// Checkins
				data = JSON.parse(json[1]);
				$("#map-canvas").show();
				var map = new google.maps.Map(document.getElementById("map-canvas"), {mapTypeId: google.maps.MapTypeId.ROADMAP});
				var bounds = new google.maps.LatLngBounds();
				
				$.each( data, function() {
					var d = new Date(0);
					d.setSeconds(this.createdAt);
					//result += d.toUTCString();
					result += "<div class='venue'>";
					result += "<img class='venueImg' src='" + "'/>";
					result += "<div class='venueContent'>";
					result += "<span class='venueName'>" + this.venue.name + ", </span>";
					result += this.venue.location.address + ", " + this.venue.location.city + "<br>";
					
					var categories = [];
					$.each( this.venue.categories, function() {
						categories.push( "<img src='" + this.icon + "' height='15' style='vertical-align:text-top' /> " + this.name );
					});
					result += categories.join(", ") + "<br>";
					result += (this.venue.url) ? "<a href='" + this.venue.url + "'>" + this.venue.url + "</a><br>" : "";
					result += (this.venue.description) ? this.venue.description : "No description available"; //TODO Get complete venue object for description
					result += "</div>";
					result += "</div>";
					
			        var marker = new google.maps.Marker({
			            position: new google.maps.LatLng(this.venue.location.lat, this.venue.location.lng),
			            map: map,
			            title: this.venue.name
			        });
			        
			        var infowindow = new google.maps.InfoWindow({
			        	content: "<div class='mapInfobox'><b>" + this.venue.name + "</b><br>"
			        		+ this.venue.location.address + ", " + this.venue.location.city + "<br>"
			        		+ "<span style='color:#aaa'>" + d.toUTCString() + "</span><br><br>"
			        		+ "<img src='" + this.user.photo + "' width='50' /> "
			        		+ "\"" + this.shout + "\""
			        		+ "</div>"
		        	});
			        
			        google.maps.event.addListener(marker, 'click', function() {
			        	infowindow.open(map,marker);
		        	});
			        
			        bounds.extend(marker.position);
			        
				});
				$("#dynamicText").html(result);
				map.fitBounds(bounds);
			},
			error: function(xhr,textStatus,errorThrown){
				$("#dynamicText").html(errorThrown);
			}
		});
	});
	
	$("#form4Submit").click(function() {
		var parameters = "formId=" + encodeURIComponent(document.getElementsByName("formId")[3].value);
		loadXMLDoc(parameters);
	});
	
	$(".results").on('click', '.retweets', function(e) {	
		var tweet = $(this).parent().parent();
		var tweetId = tweet.find('.tweetId').val();

		$.ajax({
			url: 'Servlet',
			type: 'post',
			datatype: 'json',
			data: tweet.find('.retweetersForm').serialize(),
			success: function(data){
				
				var result = "";
				$.each( JSON.parse(data), function() {
					result += "<img class='tweetImgSmall' src='" + this.profileImageUrl + "'/>";
					//result += this.name + " @" + this.screenName + "<br>";
				});
				alert("Hi Ben!");
				if (!result) {
					$("#retweetsFor" + tweetId).html("No retweets!");
				} else {
					$("#retweetsFor" + tweetId).html(result);
				}

			},
			error: function(xhr,textStatus,errorThrown){
				$("Error finding retweeters").insertAfter(tweet);
			}
		});
		
		e.preventDefault();
		
	});
	
});