import React, { Component } from 'react';
import './App.css';

export const handleResponse = (response) => {
  return response.json().then(json => {
      return response.ok ? json : Promise.reject(json);
    });
} 

export default class App extends Component {

  constructor(props) {
    super(props);

    this.titleRef = React.createRef();
    this.translateRef = React.createRef();

    this.state = {
      words: [],
      title: [],
      translate: [],
    };
  }

  componentDidMount() {
    this.getWords();
  }

  getWords() {
    fetch('https://dota-dictionary.herokuapp.com/api/words')
    .then((res) => res.json())
    .then((resJson) => {
      this.setState({words: resJson.words})
    })
      .catch(e => {
        console.log('Error: ' + e);
      })
  }

  CreateWord = e => {
    e.preventDefault();
    console.log(this.titleRef.current.value);
    console.log(this.translateRef.current.value);
    fetch(`https://dota-dictionary.herokuapp.com/api/words/add?title=${this.titleRef.current.value}&translate=${this.translateRef.current.value}`, {
      method: 'post',
     })
     .then(
      this.getWords()
    )
    .catch(e => {
      console.log('Error:' + e);
    })
  }

  render() {
    const { words} = this.state
    return (
      <div>
        <header className="app-header">
          <h1 className="app-title">DotaDictionary</h1>
        </header>
        <blockquote>Word CRUD. На фронте пока лишь create, readall. Бэк - все остальное</blockquote>
        <form>
        <h4>Создать</h4>
        <h5>Слово</h5><input className="text2" type="text" ref={this.titleRef} />
        <h5>Перевод</h5><input className="text2" type="text" ref={this.translateRef} />
        <br/>
        <input className="button2" type="button" value="Создать слово" onClick={this.CreateWord} />
        <br/>
        </form>

        <h4>Json</h4>
            {words.map(word => <ul> <li>Title: {word.title} </li> <li>Translate: {word.translate} </li> <li>Quantity: {word.quantity}</li> </ul>)}
      </div>
    );
  }
}
