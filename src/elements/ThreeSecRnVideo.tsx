/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

import * as React from 'react';
import type { NativeMethods } from 'react-native';
import { StyleSheet } from 'react-native';
import BaseComponent from '../base/BaseComponent';
import androidGrfVidPr from '../fabric/AndroidThreeSecVideoViewNativeComponent';
import { isAndroid } from '../utils/helper';

const styles = StyleSheet.create({
  video: {
    height: 452,
    width: '100%',
    borderWidth: 0,
  },
});
const defaultStyle = styles.video;

export interface ThreeSecRnVideoProps {
  source?: string;
  isPlay?: boolean;
  volume?: number;
  playerWidth?: number;
  playerHeight?: number;
  resizeMode?: number;
  isReplay?: boolean;
}

export default class ThreeSecRnVideo extends BaseComponent<ThreeSecRnVideoProps> {
  private labelLog = '[ThreeSecRnVideo]';
  static displayName = 'ThreeSecRnVideo';

  static defaultProps = {
    source: isAndroid() ? '' : '',
    playerHeight: 500,
    playerWidth: 350,
    volume: 0,
    isReplay: true,
  };

  constructor(props: ThreeSecRnVideoProps) {
    super(props);
    let labelLogLocal = '';
    if (__DEV__) {
      labelLogLocal = `${this.labelLog} [constructor]`;
    }
    labelLogLocal &&
      console.log(`${labelLogLocal} props -> ${JSON.stringify(props)}`);
  }

  render() {
    let labelLogLocal = '';
    if (__DEV__) {
      labelLogLocal = `${this.labelLog} [render]`;
    }
    // const { props } = this;
    const { source, isPlay, volume, playerHeight, playerWidth, isReplay } =
      this.props;
    // TODO: support only android for now as Android is sufferring a lot of performance
    const Video = isAndroid() ? androidGrfVidPr : androidGrfVidPr;

    labelLogLocal &&
      console.log(
        `${labelLogLocal} this.props -> ${JSON.stringify(this.props)}`
      );

    return (
      <Video
        style={defaultStyle}
        source={source}
        play={isPlay}
        replay={isReplay}
        volume={volume}
        playerHeight={playerHeight}
        playerWidth={playerWidth}
        ref={(ref) =>
          this.refMethod(ref as (ThreeSecRnVideo & NativeMethods) | null)
        }
      />
    );
  }
}
