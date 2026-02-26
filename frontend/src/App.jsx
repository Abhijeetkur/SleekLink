import React, { useState } from 'react';
import { Link2, Copy, Check, ArrowRight, Zap, Shield, BarChart3, Activity, LogOut } from 'lucide-react';
import './App.css';
import Analytics from './Analytics';
import Auth from './Auth';

function App() {
    const [token, setToken] = useState(localStorage.getItem('token') || '');
    const [activeTab, setActiveTab] = useState('shorten');
    const [url, setUrl] = useState('');
    const [shortUrl, setShortUrl] = useState('');
    const [isCopied, setIsCopied] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const handleShorten = async (e) => {
        e.preventDefault();
        if (!url) return;

        setIsLoading(true);

        try {
            const response = await fetch(`http://${window.location.hostname}:8080/shorten`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ longUrl: url })
            });

            if (!response.ok) {
                throw new Error('Error shortening URL');
            }

            const shortCode = await response.text();
            setShortUrl(`http://localhost:8080/${shortCode}`);
        } catch (error) {
            console.error('Error:', error);
            alert('Failed to shorten URL. Is the backend running?');
        } finally {
            setIsLoading(false);
        }
    };

    const handleCopy = () => {
        navigator.clipboard.writeText(shortUrl);
        setIsCopied(true);
        setTimeout(() => setIsCopied(false), 2000);
    };

    return (
        <div className="app-wrapper">
            <div className="bg-orbs">
                <div className="orb orb-1"></div>
                <div className="orb orb-2"></div>
            </div>

            <nav className="navbar">
                <div className="logo">
                    <Link2 className="logo-icon" size={28} />
                    <span>SleekLink</span>
                </div>
                <div className="nav-links">
                    <button
                        className={`nav-tab ${activeTab === 'shorten' ? 'active' : ''}`}
                        onClick={() => setActiveTab('shorten')}
                    >
                        <Link2 size={18} /> Shortener
                    </button>
                    <button
                        className={`nav-tab ${activeTab === 'analytics' ? 'active' : ''}`}
                        onClick={() => setActiveTab('analytics')}
                    >
                        <Activity size={18} /> Analytics
                    </button>
                    {token ? (
                        <button
                            className="nav-tab"
                            onClick={() => {
                                localStorage.removeItem('token');
                                setToken('');
                                setActiveTab('shorten');
                            }}
                        >
                            <LogOut size={18} /> Logout
                        </button>
                    ) : (
                        <button
                            className={`nav-tab ${activeTab === 'auth' ? 'active' : ''}`}
                            onClick={() => setActiveTab('auth')}
                        >
                            Login
                        </button>
                    )}
                </div>
            </nav>

            <main className="main-content">
                {activeTab === 'shorten' ? (
                    <>
                        <header className="hero">
                            <div className="hero-badge">New: Custom Aliases Available! 🎉</div>
                            <h1>Shorten Your Links.<br /><span className="gradient-text">Expand Your Reach.</span></h1>
                            <p>Transform long, complicated URLs into sleek, manageable links. Perfect for social media, marketing campaigns, and everyday sharing.</p>
                        </header>

                        <section className="shortener-box">
                            <form className="shorten-form" onSubmit={handleShorten}>
                                <div className="input-group">
                                    <Link2 className="input-icon" size={22} />
                                    <input
                                        type="url"
                                        placeholder="Paste your long URL here (e.g., https://example.com/very/long/path)"
                                        value={url}
                                        onChange={(e) => setUrl(e.target.value)}
                                        required
                                    />
                                </div>
                                <button type="submit" className="btn-shorten" disabled={isLoading}>
                                    {isLoading ? 'Shortening...' : (
                                        <>Shorten <ArrowRight size={18} /></>
                                    )}
                                </button>
                            </form>

                            {shortUrl && (
                                <div className="result-container active">
                                    <div className="result-details">
                                        <span className="label">Your sleek link:</span>
                                        <a href={shortUrl} target="_blank" rel="noreferrer" className="short-url">
                                            {shortUrl}
                                        </a>
                                    </div>
                                    <button className="btn-copy" onClick={handleCopy} aria-label="Copy to clipboard">
                                        {isCopied ? <Check size={20} className="check-icon" /> : <Copy size={20} />}
                                        <span>{isCopied ? 'Copied!' : 'Copy'}</span>
                                    </button>
                                </div>
                            )}
                        </section>

                        <section className="features-section" id="features">
                            <div className="feature-card">
                                <div className="feature-icon-wrapper">
                                    <Zap size={24} className="feature-icon" />
                                </div>
                                <h3>Lightning Fast</h3>
                                <p>Our global CDN ensures your links redirect seamlessly without delay.</p>
                            </div>
                            <div className="feature-card">
                                <div className="feature-icon-wrapper">
                                    <Shield size={24} className="feature-icon" />
                                </div>
                                <h3>Secure & Reliable</h3>
                                <p>Every link is scanned for malware to ensure safety for you and your users.</p>
                            </div>
                            <div className="feature-card">
                                <div className="feature-icon-wrapper">
                                    <BarChart3 size={24} className="feature-icon" />
                                </div>
                                <h3>Advanced Analytics</h3>
                                <p>Track clicks, geographic data, and referrers with our detailed dashboard.</p>
                            </div>
                        </section>
                    </>
                ) : activeTab === 'analytics' ? (
                    token ? (
                        <Analytics token={token} />
                    ) : (
                        <div className="empty-state" style={{ marginTop: '5rem' }}>
                            <Shield size={48} style={{ margin: '0 auto', marginBottom: '1rem', opacity: 0.5 }} />
                            <h3>Authentication Required</h3>
                            <p style={{ marginBottom: '1.5rem', marginTop: '0.5rem', color: 'var(--text-muted)' }}>Please log in to view the analytics dashboard.</p>
                            <button className="btn-primary" onClick={() => setActiveTab('auth')}>
                                Go to Login
                            </button>
                        </div>
                    )
                ) : activeTab === 'auth' ? (
                    <Auth
                        setToken={(t) => {
                            setToken(t);
                            setActiveTab('analytics');
                        }}
                    />
                ) : null}
            </main>

            <footer className="footer">
                <p>&copy; {new Date().getFullYear()} SleekLink. All rights reserved.</p>
            </footer>
        </div>
    );
}

export default App;
