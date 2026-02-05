import { BrowserRouter,Route,Routes } from "react-router";

//Route Components
import Home from "./components/home";
import Contract from "./components/contract";
import Header from "./components/header";

const Router = () => {
    return(
        <BrowserRouter>
        <Header/>
            <Routes>
                <Route path="/" element={<Home/>}/>
                <Route path="/contract" element={<Contract/>}/>
            </Routes>
        </BrowserRouter>
    )
}

export default Router;