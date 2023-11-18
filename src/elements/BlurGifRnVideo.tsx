/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

import * as React from 'react';
import type { NativeMethods } from 'react-native';
import { StyleSheet } from 'react-native';
import BaseComponent from '../base/BaseComponent';
import androidGifVidPr from '../fabric/AndroidBlurGifViewNativeComponent';
import { isAndroid } from '../utils/helper';

const styles = StyleSheet.create({
  video: {
    height: 452,
    width: '100%',
    borderWidth: 0,
  },
});
const defaultStyle = styles.video;

export interface BlurGifRnVideoProps {
  source?: string;
}

export default class BlurGifRnVideo extends BaseComponent<BlurGifRnVideoProps> {
  private labelLog = '[BlurGifRnVideo]';
  static displayName = 'BlurGifRnVideo';

  static defaultProps = {
    source: isAndroid() ? '' : '',
  };

  constructor(props: BlurGifRnVideoProps) {
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
    const { source } = this.props;
    // TODO: support only android for now as Android is sufferring a lot of performance
    const Gif = isAndroid() ? androidGifVidPr : androidGifVidPr;

    labelLogLocal &&
      console.log(
        `${labelLogLocal} this.props -> ${JSON.stringify(this.props)}`
      );

    return (
      <Gif
        style={defaultStyle}
        source={source}
        ref={(ref) =>
          this.refMethod(ref as (BlurGifRnVideo & NativeMethods) | null)
        }
      />
    );
  }
}
