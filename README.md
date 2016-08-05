# turnstile
turnstile is an abstract task queue which allows for long running, parallel task execution. This library provides the flexibility to define your own stateful tasks and manage their transition from state to state.

## Contents
* [Features](#features)
* [Why Write Another Task Queue?](#why)
    - [What Else Is Out There](#what-else-is-out-there)
* [Use Cases](#use-cases)
* [Getting Started](#getting-started)
    - [Gradle](#gradle)
    - [Submodule](#submodule)
* TODO: Put in a section going over the example
* [Contact Us](#contact-us)
    - [Found an Issue?](#found-an-issue)
    - [Want to Contribute?](#want-to-contribute)
    - [Questions?](#questions)
* [License](#license)

## Features
* Long running background task execution through a [Service](https://developer.android.com/reference/android/app/Service.html)
    - Automatically resume tasks after your app is killed or the device is restarted
    - Highly customizable notification (optional)
* Parallel processing
    - Ability to specify number of concurrent tasks
* Disk backed task cache
* Progress broadcasting
* Specify conditions necessary to run your task
    - Ex. Automatically pause when network is lost and resume when network has returned
* Customizable

## Why?
Although there are many phenomenal task queue libraries that exist for Android, we created this one to accommodate a broader set of requirements. While implementing upload and download for Vimeo, we found we needed more control over the state of our tasks which other libraries couldn't accommodate. For upload, we didn't want to persist the tasks to disk until the user hit a button, but we still wanted it to execute. For download, we wanted a customizable app drawer notification and the ability to store more objects with our task (like our concrete Video object). And for both queues, we wanted to support complex retry logic that allowed us to pick up where we left off so that if our upload or download was interrupted at 80%, it would be able to continue from that point without needing to start over.

There are many common [features](#features) that every task queue will have, but you may still want the flexibilty to determine how, why, and when your tasks are run. This library provides that flexibility while removing a majority of the boilerplate. It's easy to use for a simple case and extensible for a more custom [use case](#use-cases).

### What Else Is Out There
If you don't think you'll benefit from the customizability of this library, there are other options that each provide different advantages (this list is not exhaustive).

* android-priority-jobqueue
    - Pros: Most of the [features above](#features). Additionally has prioritization of jobs, job delay, load balancing, and grouping
    - Cons: Doesn't run in a Service (dies with your application). It's difficult to extend or customize.
* tape
    - Pros: Most of the [features above](#features). Very easy to implement for simple tasks.
    - Cons: No progress broadcasting or awareness of network. It's difficult to extend or customize.
* robospice
    - Pros: Good for long running network requests.
    - Cons: It's difficult to extend or customize. It also has a limited feature set.

## Use Cases
* Upload/download
* Image/video processing
* Batching important network requests when offline
    - Analytics
    - Messaging applications
* Long running background server syncing

## Getting Started
For a more in depth look at the usage, refer to the [example Android app](example). The example project includes implementation of all of the below features.

#### Gradle
Specify the dependency in your `build.gradle` file (make sure `jcenter()` is included as a repository)
```groovy
compile 'com.vimeo.turnstile:turnstile:0.8.0'
```

#### Submodule
We recommend using JCenter, but if you'd like to use the library as a submodule:
```
git submodule add git@github.com:vimeo/turnstile-android.git
```
Then in your `build.gradle` use:
```groovy
compile project(':turnstile-android:turnstile')
```

## API/Example usage

API explanation will reference the sample app, please see that code to understand it fully.

The API consists of 5 main classes, `BaseTaskManager`, `BaseTaskService`, `BaseTask`, `TaskLogger`, and `Conditions`.
- `BaseTaskManager`: This is the class responsible for running the tasks and tieing everything together. You must extend this in order to provide a name for the manage and to provide the task class. Your task manager must be a singleton, since a reference is required by your `BaseTaskService` class. See below for more information on initialization.
- `BaseTask`: The task that you wish to run. The main thing you should override is the `execute()` method. This is where you should do all your work, and it is called on a background thread. You should call task lifecycle events where appropriate, such as `onTaskProgress(int progress)`, `onTaskFailure(TaskError error)`, and `onTaskCompleted`.
- `BaseTaskService`: This is the service that the `BaseTaskManager` will be held by and on which all work will be done. You must extend this class and supply your `BaseTaskManager` instance, and notification setup data such as icons, strings, and ids.
- `TaskLogger`: TaskLogger is used to log messages and debugging information by the library and by default uses `DefaultLogger`, which uses `android.util.Log`. If you want to provide your own logging solution, or turn off or on certain logs, you can supply your own logger by implementing the `TaskLogger.Logger` interface and supplying that to `TaskLogger.setLogger(Logger logger)`.
- `Conditions`: This is an interface used by the library to determine whether or not the device conditions are suitable to run the tasks, e.g. network availability. You can use one of the default ones supplied, such as `NetworkConditionsBasic`, which checks for network connectivity, or `NetworkConditionsExtended`, which checks for wifi connectivity. You can also extend `NetworkConditions` to create your own network based conditions, or go completely custom by implementing your own `Conditions`, e.g. you don't want to run tasks if the battery is too low.

## Contact US

#### Found an Issue?
Please file it in the git [issue tracker](https://github.com/vimeo/turnstile-android/issues).

#### Want to Contribute?
If you'd like to contribute, please follow our guidelines found in [CONTRIBUTING.md](CONTRIBUTING.md).

#### Questions?
Tweet at us here: [@vimeoapi](https://twitter.com/vimeoapi).

Post on [Stackoverflow](http://stackoverflow.com/questions/tagged/vimeo-android) with the tag `vimeo-android`.

Get in touch [here](https://vimeo.com/help/contact).

Interested in working at Vimeo? We're [hiring](https://vimeo.com/jobs)!

## License
`turnstile-android` is available under the MIT license. See the [LICENSE](LICENSE) file for more info.
