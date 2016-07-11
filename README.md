# turnstile
turnstile is an abstract task queue which allows for long running, parallel task execution. This library provoides the flexibility to define your own stateful tasks and manage their transition from state to state.

## Contents
* [Features](#features)
* [Why Write Another Task Queue?](#why)
* [Getting Started](#getting-started)
    - [Gradle](#gradle)
    - [Submodule](#submodule)
* TODO: Put in a section going over the example
* [Use Cases](#use-cases)
* [Alternatives](#alternatives)
* [Contact Us](#contact-us)
    - [Found an Issue?](#found-an-issue)
    - [Want to Contribute?](#want-to-contribute)
    - [Questions?](#questions)
* [Other Vimeo Libraries](#other-vimeo-libraries)
* [License](#license)

## Features
* Long running background execution through a [Service](https://developer.android.com/reference/android/app/Service.html)
    - Resume tasks after your app is killed or the device is shut off
    - Highly customizable notification (optional)
* Parallel processing
    - Specify number of concurrent tasks 
* Disk backed task cache
* Progress broadcasting
* Automatically pauses and resumes for network tasks (optional)
* Extremely flexible
    - Optionally introduce state that you manage

## Why?
Although there are many phenomenal task queue libraries that exist for Android, we created this one to accommodate a broader requirement. While implementing upload and download for Vimeo, we found we needed more control over the state of our tasks which other libraries couldn't accommodate. There are many common [features](#features) that every task queue will have, but you may still want the flexibilty to determine how, why, and when your tasks are run. This library provides that flexibility while removing a majority of the boilerplate. It's easy to use for a simple case and open to be extended for a more custom [use case](#use-cases).

## Getting Started
For a more in depth look at the usage, refer to the [example Android app](example). The example project includes implementation of all of the below features.

### Gradle
Specify the dependency in your `build.gradle` file (make sure `jcenter()` is included as a repository)
```groovy
compile 'com.vimeo.turnstile:turnstile:0.8.0'
```

### Submodule
We recommend using JCenter, but if you'd like to use the library as a submodule:
```
git submodule add git@github.com:vimeo/turnstile-android.git
```
Then in your `build.gradle` use:
```groovy
compile project(':turnstile-android:turnstile')
```

## TODO: Section going over example
Cover utilization of each feature as well as initialization. Reference the code in the example for ease of understanding.

## Use Cases
* Upload/download
* Image/video processing
* Batching important network requests when offline
    - Analytics
    - Messaging applications
* Long running background server syncing

## Alternatives
If you don't think you'll benefit from the customizability of this library, there are other options that each provide different advantages.

TODO: maybe make a chart of features, a 1-5 ease to implement, and 1-5 for level of extensibility.

### android-priority-jobqueue
A similar featureset as specified [above](#features) with the addition of prioritization of jobs, job delay, load balancing, and grouping. Since it doesn't run in a Service, it will die with your application. Additionally, it is difficult to extend or breach from the execution contract.

### tape
A similar featureset as [above](#features) with the exception of progress broadcasting and any awareness of network. It is one of the easiest libraries to consume for simple tasks, but provides limited extensibility.

### robospice
A library optimized for long running network requests. Limited extensibility.

## Contact US

### Found an Issue?
Please file it in the git [issue tracker](https://github.com/vimeo/turnstile-android/issues).

### Want to Contribute?
If you'd like to contribute, please follow our guidelines found in [CONTRIBUTING.md](CONTRIBUTING.md).

### Questions?
Tweet at us here: [@vimeoapi](https://twitter.com/vimeoapi).

Post on [Stackoverflow](http://stackoverflow.com/questions/tagged/vimeo-android) with the tag `vimeo-android`.

Get in touch [here](https://vimeo.com/help/contact).

Interested in working at Vimeo? We're [hiring](https://vimeo.com/jobs)!

## Other Vimeo Libraries
* [vimeo-networking](link)
    - Java library to interact with the Vimeo API
* [stag](link)
    - Annotation library for generating custom type adapters with GSON

## License
`turnstile-android` is available under the MIT license. See the [LICENSE](LICENSE) file for more info.
