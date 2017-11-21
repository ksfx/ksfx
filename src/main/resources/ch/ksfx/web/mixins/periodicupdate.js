Tapestry.PeriodicUpdater = Class.create({
	
	initialize: function(options)
	{
		this.options = options;
		this.period = 10; //this.options.period;
		this.element = this.options.id;
		this.url = this.options.uri;
		
		this.start();
	},
	
	start: function()
	{
		this.onUpdate = this.updateComplete.bind(this);
		this.timer = this.onTimerEvent.bind(this).delay(this.period);
	},
	
	stop: function()
	{
		this.onUpdate = undefined;
		clearTimeout(this.timer);
	},
	
	updateComplete: function()
	{
		this.timer = this.onTimerEvent.bind(this).delay(this.period);
	},
	
	onTimerEvent: function()
	{
		var zoneObject = Tapestry.findZoneManagerForZone(this.element);
		
		if (!zoneObject) {
			return;
		}
		
		zoneObject.updateFromURL(this.url);
		
		(this.onUpdate || Prototype.emptyFunction).apply(this, arguments);
	}
	
});

Tapestry.Initializer.periodicupdater = function(options)
{
	$T(options.id).periodicupdater = new Tapestry.PeriodicUpdater(options);
};