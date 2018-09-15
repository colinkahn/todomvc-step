# TodoMVC done with re-frame and protocol55.step.alpha

This is a modified version of the original todomvc example included in the
re-frame repo. It uses
[protocol55.step.alpha](https://github.com/protocol55/step) to spec the various
states of the apps db and validate transitions after events occur. It does this
by only modifying one function and adding additional specs.

See the following files-of-interest (new code is highlighted):
- [src/todomvc/events.cljs](https://github.com/colinkahn/todomvc-step/blob/38265518d201257ac443ae0952178b14efb9ff55/src/todomvc/events.cljs#L49-L66)
- [src/todomvc/db.cljs](https://github.com/colinkahn/todomvc-step/blob/6cd71de6ca3f18ddc41a461a45afeb3b9417b2cf/src/todomvc/db.cljs#L38-L105)

See the original README (below) for details on running it. Once you have it
running open the devtools console and after each action you'll see the current
[state action state'] transition step that has been conformed via the step spec.

# TodoMVC done with re-frame

A [re-frame](https://github.com/Day8/re-frame) implementation of [TodoMVC](http://todomvc.com/).

But this is NOT your normal, lean and minimal todomvc implementation, 
geared towards showing how easily re-frame can implement the challenge.
 
Instead, this todomvc example has evolved into more of a teaching tool 
and we've thrown in more advanced re-frame features which are not 
really required to get the job done. So lean and minimal is no longer a goal. 


## Setup And Run

1. Install [Leiningen](http://leiningen.org/)  (plus Java).

2. Get the re-frame repo
   ```
   git clone https://github.com/Day8/re-frame.git
   ```

3. cd to the right example directory
   ```
   cd re-frame/examples/todomvc
   ```

4. Clean build
   ```
   lein do clean, figwheel
   ```

5. Run
   You'll have to wait for step 4 to do its compile, and then:
   ```
   open http://localhost:3450
   ```


## Compile an optimised version

1. Compile
   ```
   lein do clean, with-profile prod compile
   ```

2. Open the following in your browser
   ```
   resources/public/index.html
   ```


## Exploring The Code

From the re-frame readme:
```
To build a re-frame app, you:
  - design your app's data structure (data layer)
  - write and register subscription functions (query layer)
  - write Reagent component functions (view layer)
  - write and register event handler functions (control layer and/or state transition layer)
```

In `src`, there's a matching set of files (each small):
```
src
├── core.cljs         <--- entry point, plus history
├── db.cljs           <--- data related  (data layer)
├── subs.cljs         <--- subscription handlers  (query layer)
├── views.cljs        <--- reagent  components (view layer)
└── events.cljs     <--- event handlers (control/update layer)
```

## Further Notes

The [official reagent example](https://github.com/reagent-project/reagent/tree/master/examples/todomvc). 
