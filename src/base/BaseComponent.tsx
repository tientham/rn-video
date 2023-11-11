/**
 * Copyright: Tô Minh Tiến - GreenifyVN (tien.tominh@gmail.com)
 */

import { Component } from 'react';
import type { NativeMethods } from 'react-native';

export default class BaseComponent<P> extends Component<P> {
  root: (BaseComponent<P> & NativeMethods) | null = null;

  constructor(props: Readonly<P> | P) {
    super(props);
  }

  refMethod: (instance: (BaseComponent<P> & NativeMethods) | null) => void = (
    instance: (BaseComponent<P> & NativeMethods) | null
  ) => {
    this.root = instance;
  };

  getNativeScrollRef(): (BaseComponent<P> & NativeMethods) | null {
    return this.root;
  }

  setNativeProps = (props: P) => {
    this.root?.setNativeProps(props);
  };
}
