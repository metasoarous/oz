---
slug: a-strange-coop
title: 'A Strange Coop: How Clojure saved my Chickens from the Evil Raccoons'
published-at: '2015-06-01 04:37'
updated-at: '2015-06-01 04:53'
status: published
tags: [pin-ctrl, clojure, strange-coop, physical-computing]
uuid: 9164ba6d-4162-49ee-a4d5-109b5659342f
---


# A Strange Coop: How Clojure saved my Chickens from the Evil Raccoons

A couple of weeks ago now, I was honored to speak at ClojureWest about a fun project I've been working on. The video is here for those inclined:

<iframe width="560" height="315" src="https://www.youtube.com/embed/0EQMrpZb7-Y" frameborder="0" allowfullscreen></iframe>

The following post is for those interested in a textual account (with some additional details).

## Background

Last summer I began building an automatic chicken coop door for my chickens. The chicken run of the house I had moved into was not secure, and so I was locking them in at night, and letting them out in the morning, which was getting annoying: I couldn't stay out late, and I had to get up early every morning. My solution to this problem was to have a system that monitored the ambient light levels to determine when it needed to open/close the door every morning/night.

I set about this project with a BeagleBone Black (BBB) I had gotten some months before for tinkering. Having [fallen in love with Clojure](http://www.metasoarous.com/how-i-fell-in-love-with-clojure/), and being interested in what hardware programming was like with it, I set about the path of trying to write the programming logic in Clojure.

## Physical set up

The pieces of this system:

* Old 9V electric drill motor
* H-bridge for motor control (you could buy as a single circuit, but I built one from some relays, because I'm a glutton for punishment...)
* Handful of resistors for padding LEDs, buttons and AIN sensor circuits
* Photoresistor for detecting light
* Large 6V battery (I was amazed at how well this thing held up to the drill; lasted 6 mo before needing to be replaced)
* 2 buttons for detecting door open/close
* Spark fun BBB proto cape
* A few other odds and ends (wire, solder, wood stock for doors, etc.)

(I may eventually put together and share a schematic/diagram with [Fritzing](http://fritzing.org/download/?donation=0), but I'm a bit busy ATM).

## Clojure on BBB

Unfortunately, there weren't a lot of very thorough resources for how to do this out there specific to BBB. But a [key blog post](http://thelibraryofcongress.s3.amazonaws.com/beagleboneled.html) helped me realize that there was a simple filesystem based API for writing to and reading from pin states for the digital (GPIO) and analog pins. Not finding an existing library, I started writing some functionality that would take care of this logic for me.

This turned out not to be that difficult. The messiest part was that there was a rather nontrivial mapping between the physical pin numbers of the BBB and the GPIO pin numbers as understood internally within the board. Fortunately, I was able to find this information in an HTML table somewhere online, which I managed to munge into a CSV file I loaded into the application (FWIW, this experience working with CSV data in Clojure is part of what inspired me to write [semantic-csv](http://www.metasoarous.com/presenting-semantic-csv/)).

### Pains with (+ lein root)

There was also some additional complexity involved in getting Leiningen running properly on the board. The BBB has only a root user, and Leiningen doesn't like to run as root. One option is to set an environment variable (`LEIN_ROOT=1`) that lets it run as root without complaining. But really, it's better Unix practice not be running everything as root anyway. So I went the path of trying to get a non-root user set up. Normally this wouldn't be so difficult, but only root has full permissions on the files representing the GPIO and AIN pins. And for whatever reason, these permissions get wiped on boot. So the only way to get this working was to set up a udev rule that at startup time adds permissions for a particular group to all the necessary files. Making the new user a member of that group sealed the deal.

In case you feel like going this route, the [udev rules](https://github.com/metasoarous/strange-coop/blob/master/etc/80-gpio.rules) I came up with should be installed to `/etc/udev/rules.d/80-gpio.rules`, and the script that does [the bulk of the work](https://github.com/metasoarous/strange-coop/blob/master/etc/fix_udev_gpio.sh) to `/bin/fix_udev_gpio.sh`. Whatever non-root user you create should be added to the `gpio` group. Note that this script also activates the AIN pins for the board; not sure if I should have just set the permissions and activated the pins manually through the code, but figured it wouldn't hurt so much.

## The code

The code itself ([as it stood at ClojureWest](https://github.com/metasoarous/strange-coop/tree/clojure-west-2015)) was fairly straight forward. Since I hadn't found anything very general purpose as far as physical computing libraries went, I tried to build with refactoring into a library in mind. As such, I wanted to use protocols and records so there would be a tidy contractual relationship to follow which would allow for alternative implementations.

### The pins

I broke down the pin functionality into separate protocols for reading, writing and lifecycle concerns.

```prettyprint lang-clojure
(defprotocol ReadablePin
  (read! [this]))

(defprotocol WriteablePin
  (write! [this val]))

(defprotocol InitablePin
  (init! [this])
  (close! [this]))
```

I won't bore you with the details of the record implementations (because they _are_ boring... and because they are available in [the repository](https://github.com/metasoarous/strange-coop/blob/clojure-west-2015/src/strange_coop/bbbpin.clj)).

### H-bridge

Based on these primitives I built an abstraction for the H-bridge circuit which controls the motor:

```prettyprint lang-clojure
(defprotocol HBridgeable
  (forward! [this])
  (reverse! [this])
  (stop! [this]))
```

H-bridges can come in a few different flavors, so this protocol was nice for dispatching the behavior. While I implemented three different kinds for fun, I only used a single implementation. ([The implementations](https://github.com/metasoarous/strange-coop/blob/clojure-west-2015/src/strange_coop/hbridge.clj)).

### The door closing/opening

This was something that started off very simple, but that quickly had to grow in complexity.

The initial implementations looked something like this:

```prettyprint lang-clojure
(defn open-door! [motor floor-btn roof-btn]
  (reverse! motor)
  (wait-till (closed? roof-btn)
    (stop! motor)))

(defn close-door! [motor floor-btn]
  (forward! motor)
  (wait-till (closed? floor-btn)
    ; Wait a little so door locks
    (Thread/sleep 500)
    (stop! motor)))
```

Very simple; we start the motor in the appropriate direction until we read the appropriate indicator button registering. The only different in the `close-door!` routine is that we want to wait just a tad bit extra after the button closes to let the door lock engage (see the video; it should make sense).

However, if something doesn't work right (the button doesn't engage, or something gets jammed), bad things can happen. I was hoping the naive implementations would "just work", but I wasn't so lucky, and with my chicken's lives at stake, I wanted to make sure everything would be ok.

Thus, I went about making things more robust! The `open-door!` function isn't too bad, since there is less that can go wrong there; the door can jam, but with the drill motor pulling up on the door it's unlikely that the button wouldn't activate. On the other hand, closing the door is both higher stakes (don't want the door staying open and raccoons getting inside) and trickier with the possibility of the door not hitting the button properly.

All we do for the door opening is set a timeout in case something weird happens:

```prettyprint lang-clojure
(defn open-door! [hb floor-btn roof-btn]
  (log "Opening door")
  (let [max-time-secs 30
        max-time-ms   (* max-time-secs 1000)
        start-time    (System/currentTimeMillis)]
    (hb/reverse! hb)
    (loop []
      (cond
        (closed? roof-btn)
          :pass
        (max-time-up max-time-ms start-time)
          (do
            (log "ERROR: Unable to shut door.")
            (update-status! :errors))
        :else (recur)))
    (hb/stop! hb)))
```

For the door closing we do a timeout _and_ look for the door hitting the roof button again, and then reverse the motor. As you can see the logic for all this is much more involved:

```prettyprint lang-clojure
(defn close-door! [hb floor-btn roof-btn]
  (log "Initiating close-door! sequence")
  (let [door-close-wait 500 ; time to wait after door closes for latches to lock
        n-retries       3
        max-time-secs   120
        max-time-ms     (* max-time-secs 1000)
        lower-with-log  (fn []
                          (log "Lowering door")
                          (hb/forward! hb))
        start-time      (System/currentTimeMillis)]
    (lower-with-log)
    (loop [tries 0]
      (cond
        ; Standard closing procedure
        (closed? floor-btn) (do (log "Stopping door")
                                (Thread/sleep door-close-wait))
        ; The final try of the above
        (and (closed? roof-btn) (> tries n-retries))
                            (do (log "ERROR: Hit roof with max number of retries. Attempting to close without worrying about btn.")
                                (update-status! :errors)
                                (hb/reverse! hb)
                                (Thread/sleep 5000)) ; exit
        ; The door hit the roof before the floor button triggered; Reverse and try again.
        (closed? roof-btn)  (do (log "WARNING: Hit roof; reeling back in and trying again.")
                                (update-status! :warnings)
                                (hb/reverse! hb)
                                (Thread/sleep 1000) ; make sure door lets go of button
                                (wait-till (or (closed? roof-btn)
                                               (max-time-up max-time-ms start-time))
                                  (log "Reeling complete; trying again.")
                                  (lower-with-log)
                                  (Thread/sleep 1000)) ; make sure does lets go of button
                                (recur (inc tries)))
        ; After a certain amount of time, just give up
        (max-time-up max-time-ms start-time)
                            (do (log "ERROR: Maxed out on time; Shutting down.")
                                (update-status! :errors))
        ; Run the loop again
        :else               (recur tries)))
    ; Last thing, make sure to stop once out of loop
    (hb/stop! hb)))
```

### Putting it all together

One of the aspects of putting everything together was a state machine for keeping track of the time of day, and orchestrating door operations on transitions between night and day. This is less trivial than the no-brainer single night/day light level threshold since the light sensor signal is noisy; we don't want the door rapidly opening and closing as the sun rises :-) The state machine gives us a simple way to have different thresholds for night and day:

```prettyprint lang-clojure
(defn time-sm [state day-fn! night-fn!]
  (let [dusk 0.03
        dawn 0.13]
    {:state state
     :trans
        {:day
            (fn [brightness]
              (if (< brightness dusk)
                ; state side effects
                (do
                  (log "Switching from day to night and running evening routine")
                  (night-fn!)
                  :night)
                ; Don't change anything
                :day))
         :night
            (fn [brightness]
              (if (> brightness dawn)
                ; state side effects
                (do
                  (log "Switching from night to day and running morning routine")
                  (day-fn!)
                  :day)
                ; Don't change anything
                :night))}}))

;; Return a new state machine with the updated state, given
;; the current light level
(defn trans-sm! [sm m]
  (assoc sm :state
    (((:trans sm) (:state sm)) m)))
```

Finally, all of the pieces in one place:

```prettyprint lang-clojure
(defn -main []
  (log "Initializing -main")
  ;; First, all the setup
  (let [floor-btn (button :P8 11 :normally-off)
        roof-btn  (button :P8 12 :normally-on)
        light-ain (ain 33)
        temp-ain  (ain 35)
        mtr-ctrl  (hb/hbridge [16 17 18] :header :P8)
        timer     (time-sm
                    (log-tr "Initial time state:" (init-state! floor-btn roof-btn light-ain))
                    (partial open-door! mtr-ctrl floor-btn roof-btn)
                    (partial close-door! mtr-ctrl floor-btn roof-btn))]

    ;; Status LED thread
    (future
      (let [status-patterns {:running  [1500 3000] ; nice steady pulse
                             :warnings [1000 1000]
                             :errors   [100 50 100 750]}
            status-led (gpio :P8 14 :out)]
        (loop []
          (blink-led status-led (status-patterns @status))
          (recur))))

    ;; Main loop
    (loop [timer timer]
      (Thread/sleep 1000)
      (let [light-level (safe-read! light-ain)]
        (log "Current levels:: light:" light-level "temp:" (safe-read! temp-ain))
        (recur (trans-sm! timer light-level))))))
```

And there you have it!

## Pin-ctrl: the abstraction library

My ambitions for general purpose abstraction library ended up exceeding my initial expectations. In particular, I became inspired to write a general purpose physical computing API which could be implemented by different libraries for different boards (BBB, RPi, Arduino over Firmata, etc.).

This work is currently still in progress, but much of the core API designed is decently fleshed out and I'm very excited about the direction things are taking. You can take a look at the progress on the [pin-ctrl GitHub repository](https://github.com/clj-bots/pin-ctrl) (and also at the [Marginalia documentation](http://clj-bots.github.io/pin-ctrl/)).

## Thoughts

Again, this has been a really fun project to work on. Some of my earliest programming experiences were with physical computing (LabView), and it was always something I enjoyed very much. Writing code that makes something in the physical world **move** is simply and truly joyful.

While I enjoyed using Clojure for this project simply because I [love using Clojure](http://www.metasoarous.com/how-i-fell-in-love-with-clojure/), I think Clojure has an important story to tell in physical computing more generally. Physical computing is inherently concurrent; one is dealing with physical things in the real world, and often cannot pretend that it's possible to just wait till something else finishes before responding to an event or signal. Clojure's brilliant focus on concurrency makes it a perfect fit for dealing with these applications.

As such, I encourage you to play around with Clojure for physical computing.

## Future work

In closing, I'd like to share a few things I have in store for the Strange Coop:

* Re-implement using pin-ctrl, once pin-ctrl is complete
* Componentify the application into subsystems
* Use core.async channels for communication between subsystems (maybe...)
* Get a network connection established to the coop's BBB
  * Network notifications, particularly of errors and warnings (via message posting to a remote server)
  * Initiate a live REPL for interactivity
  * Chicken cam feed...
* Raccoon paintball turret with OpenCV for visual recognition...
* Automatic chicken feeder...

OK; I might not get to _all_ of these, but one can dream, right? :-)

Stay posted of development on the [strange-coop master branch](https://github.com/metasoarous/strange-coop). I'll probably post here as well once some substantial progress has been made.



