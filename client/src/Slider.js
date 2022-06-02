import React, { Component } from 'react';
import { useState, useEffect } from 'react';
import { render } from 'react-dom';

export default class Slider extends React.Component {
    state = {}

    constructor(props) {
        super(props)
        this.state = {
            min: props.min,
            max: props.max,
            value: props.value
        }
    } 

    changeSlider(e) {
        this.setState({
            min: this.state.min,
            max: this.state.max,
            value: e.target.value
        })
    }

    getValue() {
        return this.state.value
    }

    render() {
        return(
            <div>
                <input type="range" onChange={this.changeSlider.bind(this)} min={this.state.min} max={this.state.max} value={this.state.value} className="slider Horizontal-list-element" id="myRange"></input>
                <p className="Horizontal-list-element Slider-label">{""+this.state.value}</p>
            </div>
        )
    }
}