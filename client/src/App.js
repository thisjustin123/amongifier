import './App.css';
import React from 'react';
import ReactDOM from 'react';
import { useState, useEffect } from 'react';
import { useRef } from 'react';
import Slider from './Slider';
import ElementRef from 'react';
import DrawableCanvas from 'react-drawable-canvas'
import LoadingSpin from 'react-loading-spin'
import worker from './worker'
import WorkerBuilder from './WorkerBuilder';

function App() {
  var [state, setState] = useState(0);
  var [canvasState, setCanvasState] = useState({ width: 100, height: 100 });
  var [checkedState, setCheckedState] = useState(false);
  var [postState, setPostState] = useState({ text: "Communicating with the server...", progress: 0, stage: 0 });
  var [boundaryState, setBoundaryState] = useState({
    x: { min: 0, max: 1 },
    y: { min: 0, max: 1 }
  })

  var points = [];
  var absolutePoints = [];
  var xs = { min: 0, max: 1 }
  var ys = { min: 0, max: 1 }

  const mainImageRef = useRef(null)
  const mainImageRef2 = useRef(null)
  const canvasRef = useRef(null)
  const canvasRef2 = useRef(null)
  const canvasRef3 = useRef(null)
  const outsideWrapperRef = useRef(null)
  const outsideWrapperRef2 = useRef(null)


  var isMouseDown = false;

  window.addEventListener('resize', () => {
    if (state.screen == 1) {
      setCanvasState({
        width: outsideWrapperRef.current.getBoundingClientRect().width,
        height: outsideWrapperRef.current.getBoundingClientRect().height
      })
    }
    else if (state.screen == 2) {
      setCanvasState({
        width: outsideWrapperRef2.current.getBoundingClientRect().width,
        height: outsideWrapperRef2.current.getBoundingClientRect().height
      })
      const canvas = canvasRef2.current
      const context = canvas.getContext('2d')

      context.fillStyle = "rgba(0, 100, 255, 0.5)";
      context.strokeStyle = '#00FF00';
      context.lineWidth = 4;

      context.beginPath();
      context.moveTo(firstPoint.x, firstPoint.y);
      for (let i = 1; i < points.length; i++) {
        var pPoint = absolutePoints[i - 1];
        context.lineTo(absolutePoints[i].x, absolutePoints[i].y);
      }
      context.moveTo(prevPoint.x, prevPoint.y)
      context.lineTo(firstPoint.x, firstPoint.y);
      context.stroke();
      context.closePath();
      context.fill();
    }
  });

  const handleFileInput = (e) => {
    if (!state.fadeOut) {
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

  const moveOnTo2 = () => {
    if (!state.fadeOut) {
      setState({
        isValid: true,
        fadeOut: true,
        fadeIn: false,
        screen: 0,
        image: state.image
      })

      setTimeout(() => {
        setState({
          isValid: false,
          fadeOut: false,
          fadeIn: true,
          screen: 1,
          image: state.image
        })

        setTimeout(() => {
          setCanvasState({
            width: outsideWrapperRef.current.getBoundingClientRect().width,
            height: outsideWrapperRef.current.getBoundingClientRect().height
          })
          console.log(outsideWrapperRef.current.getBoundingClientRect().width)
        }, 100)
      }, 800);
    }
  }

  const moveOnTo3 = () => {
    if (points.length > 0) {
      xs = { min: points[0].x, max: points[0].x }
      ys = { min: points[0].y, max: points[0].y }
      points.forEach((p) => {
        if (p.x < xs.min) {
          xs.min = p.x
        }
        if (p.x > xs.max) {
          xs.max = p.x
        }
        if (p.y < ys.min) {
          ys.min = p.y
        }
        if (p.y > ys.max) {
          ys.max = p.y
        }
      })
      setBoundaryState({ x: { min: xs.min, max: xs.max }, y: { min: ys.min, max: ys.max } })
      midPoint = { x: (xs.min + xs.max) / 2, y: (ys.min + ys.max) / 2 }
    }
    else {
      midPoint = { x: .5, y: .5 }
    }
    if (!state.fadeOut) {
      console.log(points)
      setState({
        isValid: false,
        fadeOut: true,
        fadeIn: false,
        screen: 1,
        image: state.image
      })

      setTimeout(() => {
        setState({
          isValid: false,
          fadeOut: false,
          fadeIn: true,
          screen: 2,
          image: state.image
        })
        setTimeout(() => {
          const canvas = canvasRef2.current
          const context = canvas.getContext('2d')
          isMouseDown = true
          drawMidpoint(
            {
              clientX: midPoint.x * mainImageRef2.current.getBoundingClientRect().width + mainImageRef2.current.getBoundingClientRect().x,
              clientY: midPoint.y * mainImageRef2.current.getBoundingClientRect().height + mainImageRef2.current.getBoundingClientRect().y,
            }
          )
          isMouseDown = false

          context.fillStyle = "rgba(0, 100, 255, 0.5)";
          context.strokeStyle = '#00FF00';
          context.lineWidth = 4;

          context.beginPath();
          context.moveTo(firstPoint.x, firstPoint.y);
          for (let i = 1; i < points.length; i++) {
            var pPoint = absolutePoints[i - 1];
            context.lineTo(absolutePoints[i].x, absolutePoints[i].y);
          }
          context.moveTo(prevPoint.x, prevPoint.y)
          context.lineTo(firstPoint.x, firstPoint.y);
          context.stroke();
          context.closePath();
          context.fill();
        }, 100);
      }, 800);
    }
  }

  const moveOnTo4 = () => {
    if (!state.fadeOut) {
      setState({
        isValid: false,
        fadeIn: false,
        fadeOut: true,
        screen: 2,
        image: state.image
      })

      setTimeout(() => {
        setState({
          isValid: false,
          fadeOut: false,
          fadeIn: true,
          screen: 3,
          image: state.image
        })
        var myWorker = new WorkerBuilder(worker)
        console.log(JSON.stringify(makeInputJson()))

        myWorker.onmessage = (message) => {
          console.log("Message from worker: " + message.data);
        }

        myWorker.postMessage({
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'Origin':'http://localhost:3000',
          },
          body: JSON.stringify(makeInputJson()),
          mode: 'cors'
        });
      }, 800)
    }
  }

  var firstPoint = { x: -1, y: -1 }
  var prevPoint = { x: -1, y: -1 }

  function mouseDown(e) {
    absolutePoints = [];
    points = []
    isMouseDown = true
    prevPoint = { x: -1, y: -1 }
    firstPoint = { x: -1, y: -1 }

    const canvas = canvasRef.current
    const context = canvas.getContext('2d')

    context.clearRect(0, 0, canvasRef.current.getBoundingClientRect().width, canvasRef.current.getBoundingClientRect().height)

    const imageBeginX = canvasRef.current.getBoundingClientRect().x
    const imageBeginY = canvasRef.current.getBoundingClientRect().y

    firstPoint = { x: e.clientX - imageBeginX, y: e.clientY - imageBeginY }

    logPoint(e)
  }

  function mouseMove(e) {
    const imageBeginX = mainImageRef.current.getBoundingClientRect().x
    const imageEndX = imageBeginX + mainImageRef.current.getBoundingClientRect().width;
    const imageBeginY = mainImageRef.current.getBoundingClientRect().y
    const imageEndY = imageBeginY + mainImageRef.current.getBoundingClientRect().height;

    if (isMouseDown && e.clientX > imageBeginX && e.clientX < imageEndX && e.clientY > imageBeginY && e.clientY < imageEndY) {
      logPoint(e)
    }
  }

  function logPoint(e) {
    const canvasBeginX = canvasRef.current.getBoundingClientRect().x
    const canvasBeginY = canvasRef.current.getBoundingClientRect().y
    const imageBeginX = mainImageRef.current.getBoundingClientRect().x
    const imageBeginY = mainImageRef.current.getBoundingClientRect().y

    const x = e.clientX - imageBeginX
    const y = e.clientY - imageBeginY
    const canvasX = e.clientX - canvasBeginX
    const canvasY = e.clientY - canvasBeginY
    const canvas = canvasRef.current

    const context = canvas.getContext('2d')
    context.fillStyle = '#00FF00'
    context.strokeStyle = '#00FF00';

    if (prevPoint.x != -1 && prevPoint.y != -1) {
      points.push(
        {
          x: x / mainImageRef.current.getBoundingClientRect().width,
          y: y / mainImageRef.current.getBoundingClientRect().height
        }
      )

      absolutePoints.push(
        {
          x: canvasX,
          y: canvasY
        }
      )

      context.beginPath();
      context.moveTo(prevPoint.x, prevPoint.y);
      context.lineTo(canvasX, canvasY);
      context.lineWidth = 4;
      context.stroke();
    }



    //Make sure this is the last line of logPoint(e)!!
    prevPoint = { x: canvasX, y: canvasY }
  }

  function mouseUp(e) {
    isMouseDown = false

    const canvas = canvasRef.current
    const context = canvas.getContext('2d')
    context.fillStyle = "rgba(0, 100, 255, 0.5)";
    context.strokeStyle = '#00FF00';

    context.beginPath();
    context.moveTo(prevPoint.x, prevPoint.y);
    context.lineTo(firstPoint.x, firstPoint.y);
    context.lineWidth = 4;
    context.stroke();

    context.beginPath();
    context.moveTo(firstPoint.x, firstPoint.y);
    for (let i = 1; i < points.length; i++) {
      var pPoint = absolutePoints[i - 1];
      context.lineTo(absolutePoints[i].x, absolutePoints[i].y);
    }
    context.moveTo(prevPoint.x, prevPoint.y)
    context.lineTo(firstPoint.x, firstPoint.y);
    context.closePath();
    context.fill();
  }

  function handleCheck() {
    setCheckedState(!checkedState)
  }

  var midPoint = { x: -1, y: -1 }

  function enableMidpoint(e) {
    isMouseDown = true
    drawMidpoint(e)
  }

  function disableMidpoint() {
    isMouseDown = false
  }

  function drawMidpoint(e) {
    if (isMouseDown) {
      console.log("Drawing midpoint to (" + e.clientX + ", " + e.clientY + ")");
      xs = { min: boundaryState.x.min, max: boundaryState.x.max }
      ys = { min: boundaryState.y.min, max: boundaryState.y.max }

      const canvasBeginX = canvasRef2.current.getBoundingClientRect().x
      const canvasBeginY = canvasRef2.current.getBoundingClientRect().y
      const imageBeginX = mainImageRef2.current.getBoundingClientRect().x
      const imageBeginY = mainImageRef2.current.getBoundingClientRect().y
      const imageEndX = imageBeginX + mainImageRef2.current.getBoundingClientRect().width;
      const imageEndY = imageBeginY + mainImageRef2.current.getBoundingClientRect().height;

      if (e.clientX > (xs.max * (imageEndX - imageBeginX) + imageBeginX)) 
        e.clientX = (xs.max * (imageEndX - imageBeginX) + imageBeginX);
      else if (e.clientX < (xs.min * (imageEndX - imageBeginX) + imageBeginX))
        e.clientX = (xs.min * (imageEndX - imageBeginX) + imageBeginX);
      if (e.clientY > (ys.max * (imageEndY - imageBeginY) + imageBeginY))
        e.clientY = (ys.max * (imageEndY - imageBeginY) + imageBeginY)
      else if (e.clientY < (ys.min * (imageEndY - imageBeginY) + imageBeginY))
        e.clientY = (ys.min * (imageEndY - imageBeginY) + imageBeginY)

      console.log("X Bounds: " + xs.min + " to " + xs.max)
      console.log("Y Bounds: " + ys.min + " to " + ys.max)

      console.log("Absolute X Bounds: " + (xs.min * (imageEndX - imageBeginX) + imageBeginX) + " to " + (xs.max * (imageEndX - imageBeginX) + imageBeginX))
      console.log("Absolute Y Bounds: " + (ys.min * (imageEndY - imageBeginY) + imageBeginY) + " to " + (ys.max * (imageEndY - imageBeginY) + imageBeginY))

      if (true) {
        const canvas = canvasRef3.current
        const context = canvas.getContext('2d')

        context.clearRect(0, 0, canvas.getBoundingClientRect().width, canvas.getBoundingClientRect().height)



        const x = e.clientX - imageBeginX
        const y = e.clientY - imageBeginY
        const canvasX = e.clientX - canvasBeginX
        const canvasY = e.clientY - canvasBeginY
        context.fillStyle = "rgba(255, 0, 0, 1)";
        context.fillRect(canvasX - 2, canvasY - 8, 4, 16);
        context.fillRect(canvasX - 8, canvasY - 2, 16, 4);

        midPoint = {
          x: x / (imageEndX - imageBeginX),
          y: y / (imageEndY - imageBeginY)
        }
      }
    }
  }

  function makeInputJson() {

    const json = {
      
      "smooth": "" + smoothSlider.current.getValue(),
      "border": "" + borderSlider.current.getValue(),
      "midPointX": "" + midPoint.x,
      "midPointY": "" + midPoint.y,
      "points": "" + points.toString(),
      "aspectRatio": "" + checkedState.toString(),
      "image": state.image
    }

    return json
  }

  const smoothSlider = useRef(null);
  const borderSlider = useRef(null);

  return (
    <div className="App">
      <header className={"App-header"}>



        {(state.screen == 0 || state.screen == null) &&
          <div className={state.fadeOut ? "Fade-out disabled" : ""}>
            Welcome to the Amongifier!<br />
            To get started, upload an image containing a face you'd like to amongify.<br />
            <input type="file" className="Image-upload" accept=".png,.jpg,.jpeg" onChange={handleFileInput} /><br />
            <button className={state.isValid ? "Proceed-button" : "Proceed-button hidden"} onClick={moveOnTo2}>Proceed to Step 2 &gt;&gt;&gt;</button>
          </div>
        }

        {state.screen == 1 &&
          <div className={state.fadeOut ? "Fade-out disabled" : "Fade-in"}>
            Cut out the face from your image:<br />
            <p className="Hint-text">(or just hit Proceed if the face is already cut out)</p>
            <div className="outsideWrapper" ref={outsideWrapperRef}>
              <div className="insideWrapper">
                <img draggable="false" ref={mainImageRef} className={state.image.width > state.image.height ? "Main-image-wide" : "Main-image"} src={state.image}></img>
                <canvas width={canvasState.width} height={canvasState.height} ref={canvasRef} className="coveringCanvas" onMouseDown={mouseDown} onMouseUp={mouseUp} onMouseMove={mouseMove}></canvas>
              </div>
            </div>
            <p className="Hint-text">(Note: Only transparent and white backgrounds are automatically ignored by the program.</p>
            <p className="Hint-text">If your has a different colored background, you'll have to cut it out here.)</p>
            <button className={"Proceed-button"} onClick={moveOnTo3}>Proceed to Step 3 &gt;&gt;&gt;</button>
          </div>
        }

        {state.screen == 2 &&
          <div className={state.fadeOut ? "Fade-out disabled" : "Fade-in"}>
            Smoothing Level<br />
            <p className="Hint-text No-vert-padding" style={{ marginTop: 0, marginBottom: 0 }}>(Higher is better but takes longer)</p>
            <Slider ref={smoothSlider} min={1} max={15} value={8} />
            Border Blend Level<br />
            <p className="Hint-text No-vert-padding" style={{ marginTop: 0, marginBottom: 0 }}>(Higher is better but takes longer)</p>
            <Slider ref={borderSlider} min={1} max={15} value={8} />
            <div title="If checked, the program will stretch/squeeze the image to fit an aspect ratio that is likely to give good results.">
              Force good aspect ratio:
              <input type="checkbox" checked={checkedState} onChange={handleCheck} className="Check-box" />
            </div>
            <p className="No-vert-padding" style={{ marginTop: 0, marginBottom: 0 }}>Click to set midpoint</p>
            <p className="Hint-text No-vert-padding" style={{ marginTop: 0, marginBottom: 0 }}>(Click on the middle of the face)</p>
            <div className="outsideWrapper" ref={outsideWrapperRef2} style={{ paddingTop: 0, marginTop: 10 }}>
              <div className="insideWrapper">
                <img draggable="false" ref={mainImageRef2} className={state.image.width > state.image.height ? "Main-image-wide" : "Main-image"} src={state.image}></img>
                <canvas width={canvasState.width} height={canvasState.height} ref={canvasRef2} className="coveringCanvas"></canvas>
                <canvas width={canvasState.width} height={canvasState.height} ref={canvasRef3} onMouseDown={enableMidpoint} onMouseMove={drawMidpoint} onClick={disableMidpoint} className="coveringCanvas"></canvas>
              </div>
            </div>
            <button className={"Proceed-button"} onClick={moveOnTo4}>Send it in! &gt;&gt;&gt;</button>
          </div>
        }

        {state.screen == 3 &&
          <div className={state.fadeOut ? "Fade-out disabled" : "Fade-in"}>
            <p>{postState.text}</p>
            <LoadingSpin primaryColor='#005566' />
          </div>
        }
      </header>
    </div>
  );
}

export default App;
