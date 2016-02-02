
var app = angular.module("app", ["ngResource", "ngRoute", "ngAnimate"])
	.constant("apiUrl", "http://localhost:9000/api")
	.config(["$routeProvider", function($routeProvider) {
		return $routeProvider.when("/", {
			templateUrl: "/views/main",
			controller: "ListCtrl"
		}).when("/create", {
			templateUrl: "/views/detail",
			controller: "CreateCtrl"
	    }).when("/edit/:id", {
			templateUrl: "/views/detail",
			controller: "EditCtrl"
	    }).otherwise({
			redirectTo: "/"
		});
	}
	]).config([
	"$locationProvider", function($locationProvider) {
		return $locationProvider.html5Mode(true).hashPrefix("!"); // enable the new HTML5 routing and history API
	}
]);

// the global controller
app.controller("AppCtrl", ["$scope", "$location", function($scope, $location) {
	// the very sweet go function is inherited by all other controllers
	$scope.go = function (path) {
		$location.path(path);
	};
}]);

// MINE:  the list controller
app.controller("ListCtrl", ["$scope", "$resource", "$timeout", "apiUrl", 
                            function($scope, $resource, $timeout, apiUrl) {
	var date = new Date();

	$scope.datestring = function() {
		var month = date.getMonth() + 1;
		var day = date.getDate();
		return date.getFullYear() + (month < 10 ? "0" : "") + month + (day < 10 ? "0" : "") + day;
	}

	$scope.doneday = $scope.datestring();

	$scope.increasedate = function() {
		var result = new Date(date);
	    result.setDate(date.getDate() + 1);
	    date = result;
		$scope.doneday = $scope.datestring();
		var Celebrities = $resource(apiUrl + "/donelist/" + $scope.doneday); 
		$scope.celebrities = Celebrities.query(); 
	};

	$scope.decreasedate = function() {
		var result = new Date(date);
	    result.setDate(date.getDate() - 1); // increase and decrease are almost identical, except this line
	    date = result;
		$scope.doneday = $scope.datestring();
		var Celebrities = $resource(apiUrl + "/donelist/" + $scope.doneday); 
		$scope.celebrities = Celebrities.query(); 
	};

	$scope.init = function() {
		$scope.show=true;
	    var Celebrities = $resource(apiUrl + "/restaurants/"); 
	    $scope.restaurants = Celebrities.query(); 
	};
	
	$scope.init();
	
	$scope.add = function() {
		var create = $resource(apiUrl + "/restaurants/new"); // a RESTful-capable resource object
		create.save({'donetext' : $scope.donetext}); 
		$scope.donetext=''; 
		$scope.show=false;
		$timeout(function() { $scope.init();  }, 500); // go back to public/html/main.html
		//TODO should I implement a success() or simply sleep for 500ms?
	};
}]);

// the create controller
app.controller("CreateCtrl", ["$scope", "$resource", "$timeout", "apiUrl", function($scope, $resource, $timeout, apiUrl) {
	// to save a celebrity
	$scope.save = function() {
		var CreateCelebrity = $resource(apiUrl + "/restaurants/new"); // a RESTful-capable resource object
		CreateCelebrity.save($scope.celebrity); // $scope.celebrity comes from the detailForm in public/html/detail.html
		$timeout(function() { $scope.go('/'); }); // go back to public/html/main.html
	};
}]);

// the edit controller
app.controller("EditCtrl", ["$scope", "$resource", "$routeParams", "$timeout", "apiUrl", function($scope, $resource, $routeParams, $timeout, apiUrl) {
	var ShowCelebrity = $resource(apiUrl + "/celebrities/:id", {id:"@id"}); // a RESTful-capable resource object
	if ($routeParams.id) {
		// retrieve the corresponding celebrity from the database
		// $scope.celebrity.id.$oid is now populated so the Delete button will appear in the detailForm in public/html/detail.html
		$scope.celebrity = ShowCelebrity.get({id: $routeParams.id});
		$scope.dbContent = ShowCelebrity.get({id: $routeParams.id}); // this is used in the noChange function
	}  
	
	// decide whether to enable or not the button Save in the detailForm in public/html/detail.html 
	$scope.noChange = function() {
		return angular.equals($scope.celebrity, $scope.dbContent);
	};

	// to update a celebrity
	$scope.save = function() {
		var UpdateCelebrity = $resource(apiUrl + "/celebrities/" + $routeParams.id); // a RESTful-capable resource object
		UpdateCelebrity.save($scope.celebrity); // $scope.celebrity comes from the detailForm in public/html/detail.html
		$timeout(function() { $scope.go('/'); }); // go back to public/html/main.html
	};
	
	// to delete a celebrity
	$scope.delete = function() {
		var DeleteCelebrity = $resource(apiUrl + "/celebrities/" + $routeParams.id); // a RESTful-capable resource object
		DeleteCelebrity.delete();
		$timeout(function() { $scope.go('/'); }); // go back to public/html/main.html
	};
}]);




//MINE:  the dish list controller
app.controller("ListDishCtrl", ["$scope", "$resource", "$timeout", "apiUrl", 
                            function($scope, $resource, $timeout, apiUrl) {
	$scope.init = function() {
		$scope.show=true;
	    var Celebrities = $resource(apiUrl + "/dishes/" + $("#id").val()); 
	    $scope.dishes = Celebrities.query(); 
	};
	
	$scope.init();
	
	$scope.add = function() {
		var create = $resource(apiUrl + "/dishes/new"); // a RESTful-capable resource object
		create.save({'donetext' : $scope.donetext, 'restaurantID' : $("#id").val()}); 
		$scope.donetext=''; 
		$scope.show=false;
		$timeout(function() { $scope.init();  }, 500); // go back to public/html/main.html
		//TODO should I implement a success() or simply sleep for 500ms?
	};
}]);

// the create dish controller
app.controller("CreateDishCtrl", ["$scope", "$resource", "$timeout", "apiUrl", function($scope, $resource, $timeout, apiUrl) {
	// to save a celebrity
	$scope.save = function() {
		var CreateCelebrity = $resource(apiUrl + "/dishes/new"); // a RESTful-capable resource object
		CreateCelebrity.save($scope.celebrity); // $scope.celebrity comes from the detailForm in public/html/detail.html
		$timeout(function() { $scope.go('/'); }); // go back to public/html/main.html
	};
}]);


/** autocomplete from http://jqueryui.com/autocomplete/#multiple */
$(function() {
  function split( val ) {
    return val.split( /,\s*/ );
  }
  function extractLast( term ) {
    return split( term ).pop();
  }

  $( "#tags" )
    // don't navigate away from the field on tab when selecting an item
    .bind( "keydown", function( event ) {
      if ( event.keyCode === $.ui.keyCode.TAB &&
          $( this ).autocomplete( "instance" ).menu.active ) {
        event.preventDefault();
      }
    })
    .autocomplete({
      minLength: 0,
      source: function( request, response ) {
        // delegate back to autocomplete, but extract the last term
        response( $.ui.autocomplete.filter(
          availableTags, extractLast( request.term ) ) );
      },
      focus: function() {
        // prevent value inserted on focus
        return false;
      },
      select: function( event, ui ) {
        var terms = split( this.value );
        // remove the current input
        terms.pop();
        // add the selected item
        terms.push( ui.item.value );
        // add placeholder to get the comma-and-space at the end
        terms.push( "" );
        this.value = terms.join( ", " );
        return false;
      }
    });
});