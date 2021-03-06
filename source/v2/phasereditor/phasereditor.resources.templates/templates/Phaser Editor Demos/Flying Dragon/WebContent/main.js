
window.addEventListener('load', function() {

	var game = new Phaser.Game({
		"title": "Flying Dragon (Phaser Editor 2D)",
		"width": 800,
		"height": 450,
		"type": Phaser.AUTO,
		url: "https://phasereditor2d.com",
		physics: {
			default: "arcade",
			arcade: {
				gravity: {
					y : 400
				},
				debug: false
			}
		},
		scene: Boot
	});

});


class Boot extends Phaser.Scene {

	preload() {
		this.load.pack("section1", "assets/pack.json");
	}

	create() {
		this.scene.add("UI", UIScene);
		this.scene.add("Level", Level, true);
	}
}

