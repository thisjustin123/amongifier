function worker() {
    /* eslint-disable-next-line no-restricted-globals */
    self.addEventListener('message', e => {
        fetch('https://amongifier.herokuapp.com/amongifier/add', e.data)
        .then(response => {
            response.text().then(function(text) {postMessage(text)});
        })
    }, false);
}

export default worker;