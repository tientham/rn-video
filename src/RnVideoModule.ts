/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

import BaseComponent from './base/BaseComponent';
import RnVideo from './elements/RnVideo';
import ThreeSecVideoRnVideo from './elements/ThreeSecVideoRnVideo';
import ThreeSecPlayerRnVideo from './elements/ThreeSecPlayerRnVideo';
import GifRnVideo from './elements/GifRnVideo';
import BlurGifRnVideo from './elements/BlurGifRnVideo';

import {
  DroidRnVideo,
  DroidThreeSecPlayerRnVideo,
  DroidThreeSecVideoRnVideo,
  DroidGifRnVideo,
  DroidBlurGifRnVideo,
} from './fabric';

export type { RnVideoProps } from './elements/RnVideo';
export type { ThreeSecVideoRnVideoProps } from './elements/ThreeSecVideoRnVideo';
export type { ThreeSecPlayerRnVideoProps } from './elements/ThreeSecPlayerRnVideo';
export type { GifRnVideoProps } from './elements/GifRnVideo';
export type { BlurGifRnVideoProps } from './elements/BlurGifRnVideo';

export {
  BaseComponent,
  RnVideo,
  BlurGifRnVideo,
  GifRnVideo,
  ThreeSecVideoRnVideo,
  ThreeSecPlayerRnVideo,
  DroidRnVideo,
  DroidThreeSecPlayerRnVideo,
  DroidThreeSecVideoRnVideo,
  DroidGifRnVideo,
  DroidBlurGifRnVideo,
};
