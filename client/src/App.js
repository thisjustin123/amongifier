import './App.css';
import React, { useState } from 'react';

function App() {
  var [state, setState] = useState(0);
  const handleFileInput = (e) => {
    var image = e.target.files[0]
    setState({isValid: image != null})
    console.log(state.isValid)
  }

  return (
    <div className="App">
      <header className="App-header">
        Welcome to the Amongifier!<br/>
        To get started, upload an image containing a face you'd like to amongify!
        <input type="file" className="Image-upload" accept=".png,.jpg,.jpeg" onChange={handleFileInput}/>
        <button className={state.isValid ? "Proceed-button" : "Proceed-button hidden"} >Proceed to Step 2 >>></button>

      </header>
    </div>
  );
}

export default App;
