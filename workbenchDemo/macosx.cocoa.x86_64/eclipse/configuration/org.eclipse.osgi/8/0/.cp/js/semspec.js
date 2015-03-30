/*
 * ----------------------------------------------------------------------------- 
 * semspec.js:
 * ----------------------------------------------------------------------------- 
 *	Provides funcionality for the SemSpec viewer:
 *		(1) highlighting for coindexed stuff
 *  	(2) collapsible headers 
 *  
 *  Author: Luca Gilardi <lucag@icsi.berkeley.edu>
 * ----------------------------------------------------------------------------- 
 */

function do_highlighting() {
	var index2element = {};
	var indices = $$('.index')

	indices.each(function(e) {
		var c = e.innerHTML
		if (index2element[c])
			index2element[c].push(e);
		else
			index2element[c] = [e];

		e.observe('click', toggleColor);
	});

	var listItems = $$('.index');

	function toggleColor(event) {
		event = Event.extend(event)
		var toHighlight = index2element[event.element().innerHTML];

		// Effect.multiple(toHighlight, Effect.Highlight, { startcolor:
		// '#ff0000',
		// endcolor: '#ffffff' });
		reset(event);

		toHighlight.each(function(e) {
			if (e.hasClassName('highlighted'))
				e.removeClassName('highlighted');
			else
				e.addClassName('highlighted');
		});
	}

	function reset(event) {
		// event.stop();
		indices.each(function(e) {
			if (e.hasClassName('highlighted'))
				e.removeClassName('highlighted');
		});
	}

	$$('.type-name').each(function(e) {
		e.observe('click', toggleStatus);
	});

	function toggleStatus(event) {
		event = Event.extend(event);
		// alert(event.element());
		var element = $(event.element().up().next().down('div'));
		// alert(element.inspect());
		element.ancestors()[0].writeAttribute({
			width : element.getWidth()
		});
		Effect.toggle(element, 'slide');
	}
}
