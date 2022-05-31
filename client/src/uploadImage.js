'use strict';

const e = React.createElement;

class LikeButton extends React.Component {
  constructor(props) {
    super(props);
    this.state = { uploaded: false, image: null };
  }

  render() {
    return e(
      'button',
      { onClick: () => },
      'Like'
    );
  }
}