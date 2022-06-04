import {postState, setPostState} from "./App"

function worker() {
    /* eslint-disable-next-line no-restricted-globals */
    self.addEventListener('message', e => {
        fetch('https://amongifier.herokuapp.com/amongifier/add', e)
        .then(response => response.json())
        .then(response => console.log(response))
        .then(data => {
        setPostState({ text: "Reached server...", progress: 0 })
        console.log(data)
        });
    }, false);
}

export default worker;