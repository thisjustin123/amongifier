function cicularWorker() {
    /* eslint-disable-next-line no-restricted-globals */
    self.addEventListener('message', e => {
        fetch('http://localhost:8080/amongifier/request', e.data)
        .then(response => {
            response.text().then(function(text) {postMessage(text)});
        })
    }, false);
}

export default cicularWorker;