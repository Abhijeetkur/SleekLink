import React, { useState } from 'react';
import { BarChart3, Search, ArrowRight, Loader2, Globe, Calendar, Clock, MonitorSmartphone, MousePointer2, Laptop } from 'lucide-react';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
    LineChart, Line, PieChart, Pie, Cell, Legend
} from 'recharts';
import './Analytics.css';

const COLORS = ['#6366f1', '#10b981', '#3b82f6', '#8b5cf6', '#ef4444', '#f59e0b'];

export default function Analytics({ token }) {
    const [shortCode, setShortCode] = useState('');
    const [analyticsData, setAnalyticsData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const fetchAnalytics = async (e) => {
        e.preventDefault();
        if (!shortCode) return;

        // extract shortCode if user provided a full url
        let code = shortCode.trim();
        if (code.includes('/')) {
            const parts = code.split('/');
            code = parts[parts.length - 1];
        }

        setLoading(true);
        setError(null);
        setAnalyticsData(null);

        try {
            const resp = await fetch(`http://${window.location.hostname}:8080/analytics/${code}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (resp.status === 401 || resp.status === 403) {
                throw new Error('Unauthorized: Your session may have expired.');
            }
            if (!resp.ok) {
                throw new Error('Analytics not found or backend error');
            }
            const data = await resp.json();
            setAnalyticsData(data);
        } catch (err) {
            console.error(err);
            setError(err.message || 'Failed to fetch analytics');
        } finally {
            setLoading(false);
        }
    };

    const formatDaily = (dailyMap) => {
        if (!dailyMap) return [];
        return Object.keys(dailyMap)
            .sort() // Sort dates alphabetically/chronologically
            .map(date => ({
                date: date.substring(5), // Keep only MM-DD
                clicks: dailyMap[date]
            }));
    };

    const formatHourly = (hourlyMap) => {
        if (!hourlyMap) return [];
        return Object.keys(hourlyMap)
            .sort((a, b) => parseInt(a) - parseInt(b))
            .map(hour => ({
                hour: `${hour}:00`,
                clicks: hourlyMap[hour]
            }));
    };

    const formatGeo = (geoMap) => {
        if (!geoMap) return [];
        return Object.keys(geoMap).map((country, idx) => ({
            name: country || 'Unknown',
            value: geoMap[country],
            color: COLORS[idx % COLORS.length]
        })).sort((a, b) => b.value - a.value);
    };

    const formatPieData = (dataMap) => {
        if (!dataMap) return [];
        return Object.keys(dataMap).map((key, idx) => ({
            name: key || 'Unknown',
            value: dataMap[key],
            color: COLORS[idx % COLORS.length]
        })).sort((a, b) => b.value - a.value);
    };

    return (
        <div className="analytics-container">
            <div className="analytics-header">
                <h2 className="analytics-title">Real-Time Analytics</h2>
                <p className="text-muted">Track the performance of your sleek links, discover where your audience comes from, and optimize your reach.</p>

                <form onSubmit={fetchAnalytics} className="search-bar">
                    <div className="input-group" style={{ flex: 1 }}>
                        <Search className="input-icon" size={22} />
                        <input
                            type="text"
                            placeholder="Enter Short Code or SleekLink URL"
                            value={shortCode}
                            onChange={(e) => setShortCode(e.target.value)}
                            required
                        />
                    </div>
                    <button type="submit" className="btn-shorten" disabled={loading} style={{ minWidth: '130px' }}>
                        {loading ? <Loader2 className="spin" size={18} /> : (
                            <>Analyze <ArrowRight size={18} /></>
                        )}
                    </button>
                </form>
            </div>

            {loading && (
                <div className="loading-state">
                    <Loader2 size={32} className="spin" style={{ margin: '0 auto', marginBottom: '1rem', color: '#6366f1' }} />
                    Loading comprehensive analytics...
                </div>
            )}

            {error && (
                <div className="error-state">
                    Failed to load analytics: {error}
                </div>
            )}

            {!loading && !error && !analyticsData && (
                <div className="empty-state">
                    <BarChart3 size={48} style={{ margin: '0 auto', marginBottom: '1rem', opacity: 0.5 }} />
                    Enter a short code above to view detailed click statistics, geographic distribution, and time-based metrics.
                </div>
            )}

            {analyticsData && (
                <>
                    <div className="stats-grid">
                        <div className="stat-card">
                            <span className="stat-label">Total Engagements</span>
                            <span className="stat-value">{analyticsData.totalClicks || 0}</span>
                            <span className="text-muted" style={{ fontSize: '0.85rem' }}>Lifetime Clicks</span>
                        </div>
                    </div>

                    <div className="charts-grid">
                        <div className="chart-card">
                            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <Calendar size={20} /> Daily Traffic
                            </h3>
                            <div className="chart-wrapper">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={formatDaily(analyticsData.dailyAnalytics)}>
                                        <XAxis dataKey="date" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                                        <YAxis stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                                        <Tooltip
                                            cursor={{ fill: 'rgba(255,255,255,0.05)' }}
                                            contentStyle={{ backgroundColor: '#171b26', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: '#fff' }}
                                        />
                                        <Bar dataKey="clicks" fill="url(#colorClicksBar)" radius={[4, 4, 0, 0]} />
                                        <defs>
                                            <linearGradient id="colorClicksBar" x1="0" y1="0" x2="0" y2="1">
                                                <stop offset="5%" stopColor="#6366f1" stopOpacity={1} />
                                                <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0.8} />
                                            </linearGradient>
                                        </defs>
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        <div className="chart-card">
                            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <Globe size={20} /> Geographic Distribution
                            </h3>
                            <div className="chart-wrapper">
                                <ResponsiveContainer width="100%" height="100%">
                                    <PieChart>
                                        <Pie
                                            data={formatGeo(analyticsData.geoAnalytics)}
                                            cx="50%"
                                            cy="50%"
                                            innerRadius={60}
                                            outerRadius={90}
                                            paddingAngle={5}
                                            dataKey="value"
                                            stroke="none"
                                        >
                                            {formatGeo(analyticsData.geoAnalytics).map((entry, index) => (
                                                <Cell key={`cell-${index}`} fill={entry.color} />
                                            ))}
                                        </Pie>
                                        <Tooltip
                                            contentStyle={{ backgroundColor: '#171b26', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: '#fff' }}
                                        />
                                        <Legend wrapperStyle={{ fontSize: '12px', color: '#94a3b8' }} />
                                    </PieChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        <div className="chart-card" style={{ gridColumn: '1 / -1' }}>
                            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <Clock size={20} /> Hourly Heatmap
                            </h3>
                            <div className="chart-wrapper">
                                <ResponsiveContainer width="100%" height="100%">
                                    <LineChart data={formatHourly(analyticsData.hourlyAnalytics)}>
                                        <XAxis dataKey="hour" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                                        <YAxis stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                                        <Tooltip
                                            contentStyle={{ backgroundColor: '#171b26', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: '#fff' }}
                                        />
                                        <Line type="monotone" dataKey="clicks" stroke="#10b981" strokeWidth={3} dot={{ r: 4, fill: '#10b981', strokeWidth: 2, stroke: '#171b26' }} activeDot={{ r: 6 }} />
                                    </LineChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        <div className="chart-card">
                            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <MonitorSmartphone size={20} /> Devices
                            </h3>
                            <div className="chart-wrapper">
                                <ResponsiveContainer width="100%" height="100%">
                                    <PieChart>
                                        <Pie
                                            data={formatPieData(analyticsData.deviceAnalytics)}
                                            cx="50%"
                                            cy="50%"
                                            innerRadius={60}
                                            outerRadius={90}
                                            paddingAngle={5}
                                            dataKey="value"
                                            stroke="none"
                                        >
                                            {formatPieData(analyticsData.deviceAnalytics).map((entry, index) => (
                                                <Cell key={`cell-${index}`} fill={entry.color} />
                                            ))}
                                        </Pie>
                                        <Tooltip contentStyle={{ backgroundColor: '#171b26', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: '#fff' }} />
                                        <Legend wrapperStyle={{ fontSize: '12px', color: '#94a3b8' }} />
                                    </PieChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        <div className="chart-card">
                            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <Laptop size={20} /> Operating Systems
                            </h3>
                            <div className="chart-wrapper">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={formatPieData(analyticsData.osAnalytics)} layout="vertical" margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                                        <XAxis type="number" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} hide />
                                        <YAxis dataKey="name" type="category" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} width={100} />
                                        <Tooltip contentStyle={{ backgroundColor: '#171b26', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: '#fff' }} />
                                        <Bar dataKey="value" fill="#3b82f6" radius={[0, 4, 4, 0]}>
                                            {formatPieData(analyticsData.osAnalytics).map((entry, index) => (
                                                <Cell key={`cell-${index}`} fill={entry.color} />
                                            ))}
                                        </Bar>
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>

                        <div className="chart-card" style={{ gridColumn: '1 / -1' }}>
                            <h3 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                <MousePointer2 size={20} /> Browsers
                            </h3>
                            <div className="chart-wrapper" style={{ height: '350px' }}>
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={formatPieData(analyticsData.browserAnalytics)} margin={{ top: 20, right: 30, left: 20, bottom: 40 }}>
                                        <XAxis dataKey="name" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} angle={-45} textAnchor="end" />
                                        <YAxis stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                                        <Tooltip contentStyle={{ backgroundColor: '#171b26', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: '#fff' }} cursor={{ fill: 'rgba(255,255,255,0.05)' }} />
                                        <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                                            {formatPieData(analyticsData.browserAnalytics).map((entry, index) => (
                                                <Cell key={`cell-${index}`} fill={entry.color} />
                                            ))}
                                        </Bar>
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}
