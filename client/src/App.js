import './App.css';
import React, { useState } from 'react';

function App() {
  var [state, setState] = useState(0);
  const handleFileInput = (e) => {
    if (!state.fadeOut0) {
      var image = e.target.files[0]
      let reader = new FileReader();
      reader.onload = (e) => {
        setState({
          image: e.target.result,
          isValid: image != null,
          screen: 0
        });
      };
      reader.readAsDataURL(e.target.files[0]);
    }
  }

  const moveOn = () => {
    if (!state.fadeOut0) {
      setState({
        isValid: true,
        fadeOut0: true,
        screen: 0,
        image: state.image
      })

      setTimeout(() => {  
        setState({
          isValid: false,
          fadeOut0: false,
          fadeIn1: true,
          screen: 1,
          image: state.image
        })
      }, 800);
    }
  }

  return (
    <div className="App">
      <header className={"App-header"}>



        {(state.screen == 0 || state.screen == null) && 
          <div className={state.fadeOut0 ? "Fade-out disabled" : ""}>
            Welcome to the Amongifier!<br />
            To get started, upload an image containing a face you'd like to amongify!<br />
            <input type="file" className="Image-upload" accept=".png,.jpg,.jpeg" onChange={handleFileInput} /><br />
            <button className={state.isValid ? "Proceed-button" : "Proceed-button hidden"} onClick={moveOn}>Proceed to Step 2 >>></button>
          </div>
        }

        {state.screen == 1 &&
          <div className={state.fadeIn1 ? "Fade-in" : "hidden"}>
            Cut out the face from your image:
            <img className="Main-image" src={state.image}></img>
          </div>
        }
      </header>
    </div>
  );
}

export default App;
