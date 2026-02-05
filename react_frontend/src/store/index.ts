import { configureStore } from "@reduxjs/toolkit";
import usersReducer from "./reducers/users";
import postsReducer from "./reducers/posts";

export const store = configureStore({
  reducer: {
    user: usersReducer,
    post: postsReducer,
  },
});