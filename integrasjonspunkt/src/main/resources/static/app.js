import React from 'react';
import ReactDOM from 'react-dom';
import { Router, Route, Link, hashHistory, IndexRoute  } from 'react-router';
import { Col, 
            PanelGroup,
            Accordion, 
            Panel, 
            Label, 
            FormGroup, 
            InputGroup, 
            FormControl,
            ControlLabel,
            ButtonGroup,
            Glyphicon,
            Button,
            Checkbox,
            Table,
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
            received: false,
            all: true
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

    handleAll() {
        this.setState({all: !this.state.all});
    }

    handleReceived() {
        this.setState({received: !this.state.received});
    }

    render() {
        return (
            <div>
                <form>
                    <FormGroup>
                        <InputGroup>
                            <InputGroup.Addon>Filter</InputGroup.Addon>
                            <FormControl type="text" value={this.state.filter} onChange={this.handleFilter}></FormControl>
                            <InputGroup.Addon onClick={() => this.setState({filter: ''})}>X</InputGroup.Addon>
                        </InputGroup>
                        <Checkbox inline checked={this.state.all} value={this.state.all} onChange={() => this.handleAll()}>
                            All
                        </Checkbox>
                        <Checkbox inline disabled={this.state.all} value={this.state.received} onChange={() => this.handleReceived()}>
                            Completed
                        </Checkbox>
                    </FormGroup>
                </form>
                <ReceiptList receipts={this.state.receipts} filter={this.state.filter} received={this.state.received} all={this.state.all} />
            </div>
        );
    }
}


class ReceiptList extends React.Component {

    constructor(props) {
        super(props);
        this.displayName = 'ReceiptList';
        this.formatLocalDateTime.bind(this);
    }

    padZero(val) {
        if (val < 10) {
            return `0${val}`
        }
        return val;
    }

    formatLocalDateTime(d) {
        var monthVal = this.padZero(d.monthValue);
        var dayVal = this.padZero(d.dayOfMonth);
        var hourVal = this.padZero(d.hour);
        var minuteVal = this.padZero(d.minute);
        var secondVal = this.padZero(d.second);
        return `${d.year}.${monthVal}.${dayVal} ${hourVal}.${minuteVal}.${secondVal}`;
    }

    render() {
        if (!this.props.receipts) return (<div></div>);
        var k= 0;
        var receiptNodes = this.props.receipts.map((c) => {
            if (!c.conversationId.includes(this.props.filter) &&
                !c.messageReference.includes(this.props.filter) &&
                !c.messageTitle.includes(this.props.filter) &&
                !c.receiverIdentifier.includes(this.props.filter)) {
                return null;
            } 

            var isReceived = false;
            c.messageReceipts.forEach((r) => {
                if (r.status === 'READ') {
                    isReceived = true;
                }
            });

            if (!this.props.all && isReceived !== this.props.received) {
                return null;
            }
            k++;


            var statusNodes = c.messageReceipts.map((r, k) => {
                var lastUpdate = this.formatLocalDateTime(r.lastUpdate);

                return (
                    <tr key={k}>
                        <td>{k}</td>
                        <td>{r.status}</td>
                        <td>{lastUpdate}</td>
                        <td>{r.description}</td>
                    </tr>
                );
            });

            return (
                <Panel header={`${this.formatLocalDateTime(c.lastUpdate)} | ${c.conversationId}`} eventKey={k} key={k} >
                    <Label>messageId</Label>
                    <p>{c.conversationId}</p>
                    <hr style={{margin: "5px"}}/>
                    <Label>messageReference</Label>
                    <p>{c.messageReference}</p>
                    <hr style={{margin: "5px"}}/>
                    <Label>messageTitle</Label>
                    <p>{c.messageTitle}</p>
                    <hr style={{margin: "5px"}}/>
                    <Label>receiverIdentifier</Label>
                    <p>{c.receiverIdentifier}</p>
                    <hr style={{margin: "5px"}}/>
                    <Label>serviceIdentifier</Label>
                    <p>{c.serviceIdentifier}</p>
                    <hr style={{margin: "5px"}}/>
                    <Label>Status</Label>
                    <br/>&nbsp;
                    <Table striped bordered condensed hover>
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Status</th>
                                <th>Last update</th>
                                <th>Description</th>
                            </tr>
                        </thead>
                        <tbody>
                            {statusNodes}
                        </tbody>
                    </Table>
                </Panel>
            );
        });

        return (
            <PanelGroup accordion defaultActiveKey={1}>
                {receiptNodes}
            </PanelGroup>
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
                        <InputGroup bsSize="sm">
                            <ControlLabel>MessageId</ControlLabel>
                            <FormControl type="text"></FormControl>
                        </InputGroup>
                        <InputGroup bsSize="sm">
                            <ControlLabel>Timestamp from</ControlLabel>
                            <FormControl type="text"></FormControl>
                        </InputGroup>
                        <InputGroup bsSize="sm">
                            <ControlLabel>Timestamp to</ControlLabel>
                            <FormControl type="text"></FormControl>
                        </InputGroup>
                        <InputGroup bsSize="sm">
                            <ControlLabel>ServiceIdentifier</ControlLabel>
                            <FormControl componentClass="select" placeholder="DPO">
                                <option value="DPO">DPO</option>
                                <option value="DPV">DPV</option>
                                <option value="DPI">DPI</option>
                            </FormControl>
                        </InputGroup>
                        <FormGroup>
                            <ControlLabel>Received</ControlLabel>
                            <br/>
                            <Checkbox inline>
                                true
                            </Checkbox>
                            <Checkbox inline>
                                false
                            </Checkbox>
                        </FormGroup>
                        <Button type="button">Search</Button>&nbsp;
                        <Button type="reset">Reset</Button>
                    </FormGroup>
                </form>
            </div>
        );
    }
}

ReactDOM.render(
    <AllReceipts/>,
    document.getElementById('react')
);