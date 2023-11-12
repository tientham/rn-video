# rn-video

React Native Video

## Motivation

Some of my applications have been sufferring a lot about the performance issue with normal react native video libraries. It motivates me to make my own one for some very specific use-cases.

This library currently supports Android. And it is using the latest androidx.media3. 

Please note that, according to this annoucement of Exoplayer [Exoplayer Deprecation] (https://github.com/google/ExoPlayer#deprecation), exoplayer:2.19.1 will be the last artifact release for ExoPlayer and it will be replaced by AndroidX Media3. Therefore, my library here will survive :-)

## Installation

```sh
npm install @tientham/rn-video
```

## Usage

```ts
import { RnVideo } from "@tientham/rn-video";

// ...

<RnVideo
  source="http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
  isPlay={true}
  isReplay={true}
/>
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT Copyright (c) 2023 TO Minh Tien
