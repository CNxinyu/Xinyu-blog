import { Nav, Navbar } from 'react-bootstrap';
import { Link } from 'react-router';
const header = () => {

    return(
        <>
        <div className="container">
            <Navbar>
                <Navbar.Brand as={Link} to="/">MyApp</Navbar.Brand>
            </Navbar>
            <Nav>
                <Nav.Item>
                    <Nav.Link as={Link} to="/about">About</Nav.Link>
                </Nav.Item>
            </Nav>

        </div>
        </>
    )
}
export default header;