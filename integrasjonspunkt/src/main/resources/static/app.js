import React from 'react';
import ReactDOM from 'react-dom';
import { Router, Route, Link, hashHistory, IndexRoute  } from 'react-router';
import { Accordion, 
            Panel, 
            Label, 
            FormGroup, 
            InputGroup, 
            FormControl,
            ControlLabel,
            ButtonGroup,
            Button,
            Input } from 'react-bootstrap';
import $ from 'jquery';

class App extends React.Component {
    render() {
        return (
            <div>
                <div className="row">
                    <div className="col-md-6">
                        <div style={{textAlign: "center"}}>
                            <Link to="/all">
                                <div className="well well-lg" style={{fontSize: "30pt"}}>
                                    <i className="glyphicon glyphicon-list" style={{fontSize: "60pt"}}></i><br />
                                    Vis alle
                                </div>
                            </Link>
                        </div>
                    </div>
                    <div className="col-md-6">
                        <div style={{textAlign: "center"}}>
                            <Link to="/search">
                                <div className="well well-lg" style={{fontSize: "30pt"}}>
                                    <i className="glyphicon glyphicon-search" style={{fontSize: "60pt"}}></i><br />
                                    Finn kvittering
                                </div>
                            </Link>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}



class AllReceipts extends React.Component {

    constructor(props) {
        super(props);
        this.displayName = 'AllReceipts';
        this.state = {
            receipts: '',
            filter: '',
            filteredReceipts: ''
        };
        this.handleFilter = this.handleFilter.bind(this);
    }

    componentDidMount() {
        $.ajax({
            url: '/receipts',
            dataType: 'json',
            cache: false,
            success: function(data) {
                this.setState({receipts: data});
            }.bind(this),
            error: function(xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        }); 
    }

    handleFilter(e) {
        this.setState({filter: e.target.value});
    }

    render() {
        return (
            <div>
                <form>
                    <FormGroup>
                        <InputGroup>
                            <InputGroup.Addon>Filter</InputGroup.Addon>
                            <FormControl type="text" value={this.state.filter} onChange={this.handleFilter}></FormControl>
                        </InputGroup>
                    </FormGroup>
                </form>
                <ReceiptList receipts={this.state.receipts} filter={this.state.filter}/>
            </div>
        );
    }
}


class ReceiptList extends React.Component {
    constructor(props) {
        super(props);
        this.displayName = 'ReceiptList';
    }
    render() {
        if (!this.props.receipts) return (<div></div>);
        var receiptNodes = this.props.receipts.map((r, k) => {
            if (!r.messageId.includes(this.props.filter)) return null;
            return (
                <Panel header={r.messageId} eventKey={k} key={k}>
                    <Label>messageId</Label>
                    <p>{r.messageId}</p>
                    <hr style={{margin: "5px"}}/>
                    <Label>serviceIdentifier</Label>
                    <p>{r.targetType}</p>
                    <hr style={{margin: "5px"}}/>
                    <Label>received</Label>
                    <p>{r.received.toString()}</p>
                </Panel>
            );
        });

        return (
            <Accordion>
                {receiptNodes}
            </Accordion>
        );
    }
}


class SearchReceipts extends React.Component {

    constructor(props) {
        super(props);
        this.displayName = 'SearchReceipts';
    }

    render() {
        return (
            <div>
                <form>
                    <FormGroup>
                        <InputGroup>
                            <InputGroup.Addon>MessageId</InputGroup.Addon>
                            <FormControl type="text"></FormControl>
                        </InputGroup>
                        <InputGroup>
                            <InputGroup.Addon>Timestamp from</InputGroup.Addon>
                            <FormControl type="text"></FormControl>
                        </InputGroup>
                        <InputGroup>
                            <InputGroup.Addon>Timestamp to</InputGroup.Addon>
                            <FormControl type="text"></FormControl>
                        </InputGroup>
                        <FormGroup controlId="formControlsSelect">
                            <ControlLabel>ServiceIdentifier</ControlLabel>
                            <FormControl componentClass="select" placeholder="DPO">
                                <option value="DPO">DPO</option>
                                <option value="DPV">DPV</option>
                                <option value="DPI">DPI</option>
                            </FormControl>
                        </FormGroup>
                        <ButtonGroup title="foo">
                            <ControlLabel>Received</ControlLabel>
                            <Button active>
                                <Input ref="input1" type="radio" name="receivedRadio" value="input1" standalone defaultChecked />
                            </Button>
                            <Button active>
                                <Input ref="input2" type="radio" name="receivedRadio" value="input2"/>
                            </Button>
                        </ButtonGroup>
                    </FormGroup>
                </form>
                <SearchResultList/>
            </div>
        );
    }
}

class SearchResultList extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div>
            </div>
        );
    }
}

ReactDOM.render(
    (<Router history = {hashHistory}>
        <Route path="/" component={App}/>
        <Route path="/all" component={AllReceipts}/>
        <Route path="/search" component={SearchReceipts}/>
    </Router>),
    document.getElementById('react')
);