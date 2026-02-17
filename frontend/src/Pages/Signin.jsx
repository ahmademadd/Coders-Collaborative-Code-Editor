import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../Components/Theme';
import '../styles/Sign.css';
import '../styles/popup.css'; // Ensure you are using the correct CSS file

const SignInPage = () => {
  const { isDarkMode } = useTheme();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch('http://52.233.94.254:8080/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });
      
      const data = await response.json();
      if (response.ok && data.token) {
        localStorage.setItem('authToken', data.token);
        localStorage.setItem('email', email);
        navigate('/dashboard');
      } else {
        alert(data.message || 'Invalid credentials');
      }
    } catch (error) {
      alert('An error occurred while signing in. Please try again later.');
    }
  };

  const handleGitHubLogin = () => {
    // Redirect to GitHub OAuth authorization URL
    window.location.href = 'http://52.233.94.254:8080/oauth2/authorization/github';
  };

  const handleOAuthCallback = () => {
    const token = new URLSearchParams(window.location.search).get('token');
    const email = new URLSearchParams(window.location.search).get('email');
    
    if (token) {
      localStorage.setItem('authToken', token);
      localStorage.setItem('email', email);
      navigate('/dashboard'); // Redirect to dashboard
    }
  };

  useEffect(() => {
    handleOAuthCallback();
  }, []);


  return (
    <div className={`form-container ${isDarkMode ? 'dark-mode' : 'light-mode'}`}>
      <h2>Sign In</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="email">Email</label>
          <input
            type="email"
            id="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="password">Password</label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit">Sign In</button>
        <div className="social-buttons">
          <div className="social-button github" onClick={handleGitHubLogin}>Sign in with GitHub</div>
        </div>
      </form>
    </div>
  );
};

export default SignInPage;
