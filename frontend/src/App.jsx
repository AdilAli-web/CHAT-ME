import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  EllipsisVertical, FileText, Image as ImageIcon, LockKeyhole, LogOut,
  MessageCircle, MoreVertical, Paperclip, Phone, Plus, Search, Send, Smile,
  Sparkles, UserRound, Users, Video, X, ShieldCheck, Check, CheckCheck,
  ChevronDown, CircleHelp, Archive, Bell
} from "lucide-react";
import "./styles.css";

const API_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/$/, "");

const demoChats = [
  { userName: "rahul_sharma", displayName: "Rahul Sharma", online: true, color: "mint", time: "8:42 PM", unread: 2, preview: "Bro, pushed the latest changes 🚀", messages: [
    { id: 1, sender: "rahul_sharma", recipient: "you", body: "Hey! Did you get a chance to look at the API changes?", sentAt: "8:34 PM" },
    { id: 2, sender: "you", recipient: "rahul_sharma", body: "Yep, went through them this afternoon.", sentAt: "8:36 PM", own: true, read: true },
    { id: 3, sender: "rahul_sharma", recipient: "you", body: "Nice. The new message endpoint is looking good.", sentAt: "8:38 PM" },
    { id: 4, sender: "you", recipient: "rahul_sharma", body: "Thanks! I’m working on the UI now. Going for a clean WhatsApp-style layout.", sentAt: "8:40 PM", own: true, read: true },
    { id: 5, sender: "rahul_sharma", recipient: "you", body: "Bro, pushed the latest changes 🚀", sentAt: "8:42 PM" },
  ]},
  { userName: "sana_khan", displayName: "Sana Khan", online: true, color: "lavender", time: "7:18 PM", unread: 0, preview: "That sounds like a plan ✨", messages: [
    { id: 11, sender: "sana_khan", recipient: "you", body: "Hey! How’s your project going?", sentAt: "7:10 PM" },
    { id: 12, sender: "you", recipient: "sana_khan", body: "Almost there. Polishing the interface now.", sentAt: "7:15 PM", own: true, read: true },
    { id: 13, sender: "sana_khan", recipient: "you", body: "That sounds like a plan ✨", sentAt: "7:18 PM" },
  ]},
  { userName: "dev_community", displayName: "Dev Community", online: false, color: "peach", time: "6:51 PM", unread: 4, preview: "Ayesha: shared the new resources", messages: [
    { id: 21, sender: "dev_community", recipient: "you", body: "Ayesha: shared the new resources", sentAt: "6:51 PM" },
  ]},
  { userName: "arjun_mehta", displayName: "Arjun Mehta", online: false, color: "blue", time: "Yesterday", unread: 0, preview: "See you at the library tomorrow.", messages: [
    { id: 31, sender: "arjun_mehta", recipient: "you", body: "See you at the library tomorrow.", sentAt: "Yesterday" },
  ]},
  { userName: "priya_singh", displayName: "Priya Singh", online: true, color: "rose", time: "Yesterday", unread: 0, preview: "Haha, exactly 😂", messages: [
    { id: 41, sender: "priya_singh", recipient: "you", body: "Haha, exactly 😂", sentAt: "Yesterday" },
  ]},
  { userName: "zoya_ahmed", displayName: "Zoya Ahmed", online: false, color: "gold", time: "Mon", unread: 0, preview: "Send me the link when you can", messages: [
    { id: 51, sender: "zoya_ahmed", recipient: "you", body: "Send me the link when you can", sentAt: "Mon" },
  ]},
];

function initials(name = "?") {
  return name.split(/[\s_]+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join("");
}
function timeLabel(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}
async function request(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    credentials: "include", ...options, headers: { ...(options.headers || {}) },
  });
  if (response.status === 204) return null;
  const raw = await response.text();
  let data = null;
  try { data = raw ? JSON.parse(raw) : null; } catch { data = raw; }
  if (!response.ok) {
    const msg = typeof data === "string" ? data : data?.message || data?.detail || `Request failed (${response.status})`;
    throw new Error(msg);
  }
  return data;
}
function Avatar({ name, color = "mint", online = false, large = false }) {
  return <span className={`avatar avatar--${color} ${large ? "avatar--large" : ""}`}>
    {initials(name)}{online && <i className="avatar__online" />}
  </span>;
}
function Brand({ compact = false }) {
  return <div className={`brand ${compact ? "brand--compact" : ""}`}>
    <span className="brand__icon"><MessageCircle size={20} strokeWidth={2.5} /></span>
    {!compact && <span className="brand__name">chat<span>-me</span><small>YOUR SPACE. YOUR PEOPLE.</small></span>}
  </div>;
}

function AuthScreen({ onAuthenticated, onPreview }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ userName: "", displayName: "", password: "", phoneNo: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const update = (key) => (e) => setForm((old) => ({ ...old, [key]: e.target.value }));

  async function submit(e) {
    e.preventDefault(); setBusy(true); setError(""); setNotice("");
    try {
      const csrf = await request("/api/csrf");
      const csrfHeader = csrf?.headerName || "X-CSRF-TOKEN";
      const token = csrf?.Token || csrf?.token;
      if (mode === "register") {
        await request("/api/register", {
          method: "POST",
          headers: { "Content-Type": "application/json", ...(token ? { [csrfHeader]: token } : {}) },
          body: JSON.stringify(form),
        });
        setNotice("Account created. Sign in with your new credentials.");
        setMode("login");
        setForm((old) => ({ ...old, displayName: "", phoneNo: "" }));
      } else {
        const body = new URLSearchParams({ username: form.userName, password: form.password });
        await request("/api/login", {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded", ...(token ? { [csrfHeader]: token } : {}) },
          body,
        });
        await onAuthenticated();
      }
    } catch (err) {
      setError(err.message || "Could not connect to CHAT-ME.");
    } finally { setBusy(false); }
  }

  return <main className="auth-shell">
    <div className="auth-orb auth-orb--one" /><div className="auth-orb auth-orb--two" />
    <nav className="auth-nav"><Brand /><span className="auth-nav__secure"><ShieldCheck size={15} /> PRIVATE BY DESIGN</span></nav>
    <section className="auth-grid">
      <div className="auth-copy">
        <div className="eyebrow"><Sparkles size={14} /> A little more connected</div>
        <h1>Good conversations.<br /><em>Better connections.</em></h1>
        <p>Your people, your conversations, all in one calm place. Say hello, share a thought, keep the connection going.</p>
        <div className="auth-points">
          <span><Check size={16} /> One-to-one conversations</span>
          <span><Check size={16} /> Simple, private sign in</span>
          <span><Check size={16} /> Built for meaningful chats</span>
        </div>
        <div className="auth-quote"><div className="quote-avatars"><Avatar name="Rahul Sharma" color="mint" /><Avatar name="Sana Khan" color="lavender" /><Avatar name="Priya Singh" color="rose" /></div><p>“The best messages are the ones that bring us a little closer.”</p></div>
      </div>
      <form className="auth-card" onSubmit={submit}>
        <div className="auth-card__top"><div className="auth-card__symbol"><MessageCircle size={22} /></div><span className="auth-card__tag">WELCOME TO CHAT-ME</span></div>
        <h2>{mode === "login" ? "Welcome back" : "Create your account"}</h2>
        <p className="auth-card__sub">{mode === "login" ? "Pick up right where you left off." : "A few details, then you’re in."}</p>
        {notice && <div className="inline-notice">{notice}</div>}
        {error && <div className="inline-error">{error}<small>Make sure the Spring Boot API is running at {API_URL}.</small></div>}
        {mode === "register" && <>
          <label className="field-label">Display name</label>
          <input className="text-field" value={form.displayName} onChange={update("displayName")} placeholder="How should people see you?" maxLength={50} required />
          <label className="field-label">Mobile number</label>
          <input className="text-field" value={form.phoneNo} onChange={update("phoneNo")} placeholder="10-digit Indian mobile number" inputMode="numeric" pattern="[6-9][0-9]{9}" required />
        </>}
        <label className="field-label">Username</label>
        <div className="field-wrap"><UserRound size={17} /><input className="text-field text-field--icon" value={form.userName} onChange={update("userName")} placeholder="e.g. adil_ali" minLength={3} maxLength={24} autoComplete="username" required /></div>
        <label className="field-label">Password</label>
        <div className="field-wrap"><LockKeyhole size={17} /><input className="text-field text-field--icon" type="password" value={form.password} onChange={update("password")} placeholder="Enter your password" minLength={8} maxLength={72} autoComplete={mode === "login" ? "current-password" : "new-password"} required /></div>
        <button className="primary-button auth-submit" disabled={busy}>{busy ? <span className="spinner" /> : <>{mode === "login" ? "Sign in to CHAT-ME" : "Create account"} <span>→</span></>}</button>
        <div className="auth-toggle">{mode === "login" ? "New around here?" : "Already have an account?"} <button type="button" onClick={() => { setMode(mode === "login" ? "register" : "login"); setError(""); setNotice(""); }}>{mode === "login" ? "Create an account" : "Sign in"}</button></div>
        {mode === "login" && <button type="button" className="preview-link" onClick={onPreview}>Explore the interface with sample chats ↗</button>}
        <div className="auth-foot"><LockKeyhole size={13} /> Your password is handled securely by the backend.</div>
      </form>
    </section>
    <footer className="auth-footer"><span>© 2026 CHAT-ME</span><span>MADE FOR THE MOMENTS THAT MATTER</span><a href="https://github.com/AdilAli-web/CHAT-ME" target="_blank" rel="noreferrer">Built with care ↗</a></footer>
  </main>;
}

function App() {
  const [authenticated, setAuthenticated] = useState(false);
  const [previewMode, setPreviewMode] = useState(false);
  const [checking, setChecking] = useState(true);
  const [me, setMe] = useState(null);
  const [chats, setChats] = useState(demoChats);
  const [activeId, setActiveId] = useState("rahul_sharma");
  const [query, setQuery] = useState("");
  const [message, setMessage] = useState("");
  const [searchMode, setSearchMode] = useState("chats");
  const [people, setPeople] = useState([]);
  const [isDemo, setIsDemo] = useState(true);
  const [loadingChat, setLoadingChat] = useState(false);
  const [apiError, setApiError] = useState("");
  const [showInfo, setShowInfo] = useState(false);
  const [mobileListOpen, setMobileListOpen] = useState(true);
  const [showAttach, setShowAttach] = useState(false);
  const [filter, setFilter] = useState("all");
  const [toast, setToast] = useState("");
  const bottomRef = useRef(null);
  const inputRef = useRef(null);

  async function loadApp() {
    setChecking(true);
    try {
      const user = await request("/api/me");
      setMe(user); setAuthenticated(true); setIsDemo(false); setApiError("");
      const conversationData = await request("/api/conversations");
      const mapped = (conversationData || []).map((item) => {
        const person = item.person || {};
        const last = item.lastMessage || item.LastMessage || {};
        return { userName: person.userName, displayName: person.displayName || person.userName, color: "mint", online: false, time: timeLabel(last.sentAt || last.createdAt), unread: 0, preview: last.body || "Start a conversation", messages: [], lastMessageId: last.id };
      });
      setChats(mapped);
      setActiveId((current) => mapped.some((chat) => chat.userName === current) ? current : (mapped[0]?.userName || ""));
    } catch {
      setAuthenticated(false); setIsDemo(true);
    } finally { setChecking(false); }
  }

  useEffect(() => { loadApp(); }, []);
  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" }); }, [activeId, chats]);
  useEffect(() => {
    if (authenticated && query.trim().length >= 1) {
      const timer = setTimeout(async () => {
        try {
          const result = await request(`/api/users?query=${encodeURIComponent(query.trim())}`);
          setPeople((result || []).filter((person) => person.userName !== me?.userName));
        } catch { setPeople([]); }
      }, 250);
      return () => clearTimeout(timer);
    }
  }, [query, authenticated, me?.userName]);

  const visibleChats = useMemo(() => chats.filter((chat) => {
    const q = query.toLowerCase();
    const match = (chat.displayName || chat.userName).toLowerCase().includes(q) || (chat.userName || "").toLowerCase().includes(q) || (chat.preview || "").toLowerCase().includes(q);
    return match && (filter === "all" || (filter === "unread" ? chat.unread > 0 : filter === "online" ? chat.online : true));
  }), [chats, query, filter]);
  const active = chats.find((chat) => chat.userName === activeId) || null;
  const activeMessages = active?.messages || [];

  async function openChat(chat) {
    setActiveId(chat.userName); setMobileListOpen(false); setShowInfo(false); setQuery(""); setSearchMode("chats"); setApiError("");
    setChats((items) => items.map((item) => item.userName === chat.userName ? { ...item, unread: 0 } : item));
    if (!isDemo) {
      setLoadingChat(true);
      try {
        const history = await request(`/api/message/${encodeURIComponent(chat.userName)}?before=9223372036854775807`);
        const normalized = (history || []).map((m) => ({ ...m, own: m.sender === me?.userName, sentAt: timeLabel(m.sentAt || m.createdAt) }));
        setChats((items) => items.map((item) => item.userName === chat.userName ? { ...item, messages: normalized } : item));
      } catch (err) { setApiError(err.message); }
      finally { setLoadingChat(false); }
    }
  }

  async function startChat(person) {
    let chat = chats.find((item) => item.userName === person.userName);
    if (!chat) {
      chat = { ...person, displayName: person.displayName || person.userName, color: ["mint", "lavender", "peach", "blue", "rose"][chats.length % 5], online: false, unread: 0, time: "now", preview: "No messages yet", messages: [] };
      setChats((items) => [chat, ...items]);
    }
    await openChat(chat);
    setToast(`Chat opened with ${chat.displayName || chat.userName}`);
    setTimeout(() => setToast(""), 2600);
  }

  async function sendMessage(e) {
    e.preventDefault();
    const body = message.trim();
    if (!body || !active) return;
    setMessage("");
    if (isDemo) {
      const outgoing = { id: Date.now(), sender: "you", recipient: active.userName, body, sentAt: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }), own: true, read: true };
      setChats((items) => items.map((chat) => chat.userName === activeId ? { ...chat, messages: [...(chat.messages || []), outgoing], preview: body, time: "now" } : chat));
      return;
    }
    try {
      const csrf = await request("/api/csrf");
      const token = csrf?.Token || csrf?.token;
      const payload = await request("/api/message", {
        method: "POST",
        headers: { "Content-Type": "application/json", ...(token ? { [csrf.headerName || "X-CSRF-TOKEN"]: token } : {}) },
        body: JSON.stringify({ recipient: active.userName, body }),
      });
      const outgoing = { ...payload, own: true, sentAt: timeLabel(payload.sentAt || payload.createdAt) };
      setChats((items) => items.map((chat) => chat.userName === activeId ? { ...chat, messages: [...(chat.messages || []), outgoing], preview: body, time: outgoing.sentAt } : chat));
    } catch (err) { setMessage(body); setApiError(err.message || "Message could not be sent."); }
  }

  async function logout() {
    try {
      const csrf = await request("/api/csrf");
      const token = csrf?.Token || csrf?.token;
      await request("/logout", { method: "POST", headers: token ? { [csrf.headerName || "X-CSRF-TOKEN"]: token } : {} });
    } catch {}
    setAuthenticated(false); setPreviewMode(false); setMe(null); setIsDemo(true); setActiveId("rahul_sharma"); setShowInfo(false);
  }

  if (checking) return <main className="boot-screen"><Brand /><div className="boot-spinner" /><p>Getting your space ready…</p></main>;
  if (!authenticated && !previewMode) return <AuthScreen onAuthenticated={loadApp} onPreview={() => { setPreviewMode(true); setIsDemo(true); setChats(demoChats); setActiveId("rahul_sharma"); }} />;

  return <main className="app-shell">
    <aside className={`rail ${mobileListOpen ? "" : "rail--mobile-hidden"}`}>
      <div className="rail__brand"><Brand compact /></div>
      <div className="rail__main">
        <button className="rail-button rail-button--active" title="Chats"><MessageCircle size={21} /><span>Chats</span></button>
        <button className="rail-button" title="Contacts" onClick={() => { setSearchMode("people"); inputRef.current?.focus(); }}><Users size={21} /><span>People</span></button>
        <button className="rail-button" title="Saved" onClick={() => setToast("Saved messages are coming soon.")}><Archive size={21} /><span>Saved</span></button>
      </div>
      <div className="rail__bottom">
        <button className="rail-button" title="Help" onClick={() => setToast("CHAT-ME help: choose a chat to start messaging.")}><CircleHelp size={20} /><span>Help</span></button>
        <button className="profile-button" title={me?.displayName || me?.userName}><Avatar name={me?.displayName || me?.userName || "You"} color="mint" /></button>
      </div>
    </aside>
    <section className={`sidebar ${mobileListOpen ? "sidebar--mobile-open" : ""}`}>
      <header className="sidebar__header">
        <div><p className="eyebrow sidebar__eyebrow">YOUR INBOX</p><h1>Messages<span className="live-dot" /></h1></div>
        <div className="header-actions">
          <button className="icon-button icon-button--soft" title="New chat" onClick={() => { setSearchMode("people"); inputRef.current?.focus(); }}><Plus size={19} /></button>
          <button className="icon-button icon-button--soft" title="More options" onClick={() => setToast("You’re all caught up.")}><EllipsisVertical size={19} /></button>
        </div>
      </header>
      <div className="sidebar__search">
        <Search size={17} /><input ref={inputRef} value={query} onChange={(e) => setQuery(e.target.value)} onFocus={() => query && setSearchMode("people")} placeholder="Search people or messages" />
        {query && <button className="clear-search" onClick={() => { setQuery(""); setSearchMode("chats"); }}><X size={15} /></button>}
        <kbd>⌘ K</kbd>
      </div>
      <div className="sidebar__tabs">
        <button className={filter === "all" && searchMode === "chats" ? "tab tab--active" : "tab"} onClick={() => { setFilter("all"); setSearchMode("chats"); }}>All chats <span>{chats.length}</span></button>
        <button className={filter === "unread" ? "tab tab--active" : "tab"} onClick={() => { setFilter(filter === "unread" ? "all" : "unread"); setSearchMode("chats"); }}>Unread</button>
        <button className={searchMode === "people" ? "tab tab--active" : "tab"} onClick={() => setSearchMode("people")}>Find people</button>
      </div>
      {searchMode === "people" ? <div className="people-panel">
        <p className="section-label">{query ? "SEARCH RESULTS" : "FIND SOMEONE"}</p>
        {!query && <div className="empty-search"><span><Search size={20} /></span><strong>Who do you want to message?</strong><p>Search by username to start a new chat.</p></div>}
        {query && people.map((person) => <button className="people-result" key={person.userName} onClick={() => startChat(person)}><Avatar name={person.displayName || person.userName} color="lavender" /><span><strong>{person.displayName || person.userName}</strong><small>@{person.userName}</small></span><span className="result-arrow">↗</span></button>)}
        {query && people.length === 0 && <p className="no-results">No people found yet.</p>}
      </div> : <div className="chat-list">
        <div className="section-label">{filter === "unread" ? "UNREAD CONVERSATIONS" : "RECENT CONVERSATIONS"} <span>{visibleChats.length}</span></div>
        {visibleChats.map((chat) => <button key={chat.userName} onClick={() => openChat(chat)} className={`chat-row ${activeId === chat.userName ? "chat-row--active" : ""}`}>
          <Avatar name={chat.displayName || chat.userName} color={chat.color} online={chat.online} />
          <span className="chat-row__content"><span className="chat-row__top"><strong>{chat.displayName || chat.userName}</strong><time>{chat.time}</time></span><span className="chat-row__bottom"><span className="chat-row__preview">{chat.preview}</span>{chat.unread > 0 && <b className="unread-count">{chat.unread}</b>}</span></span>
        </button>)}
        {visibleChats.length === 0 && <div className="no-results">No conversations match that search.</div>}
      </div>}
      <footer className="sidebar__footer"><span className="privacy-icon"><ShieldCheck size={15} /></span><span><strong>Conversations stay personal</strong><small>Everyday connection, in your space.</small></span><ChevronDown size={15} /></footer>
    </section>
    <section className={`conversation ${!mobileListOpen ? "conversation--mobile-open" : ""}`}>
      {active ? <>
        <header className="conversation__header">
          <button className="mobile-back icon-button" onClick={() => setMobileListOpen(true)} aria-label="Back to chats"><span>←</span></button>
          <Avatar name={active.displayName || active.userName} color={active.color} online={active.online} />
          <button className="conversation__person" onClick={() => setShowInfo(!showInfo)}><strong>{active.displayName || active.userName}</strong><span>{active.online ? <><i className="status-dot" /> Online now</> : `@${active.userName}`}</span></button>
          <div className="conversation__actions"><button className="icon-button" title="Voice call" onClick={() => setToast("Voice calls are not connected yet.")}><Phone size={18} /></button><button className="icon-button" title="Video call" onClick={() => setToast("Video calls are not connected yet.")}><Video size={19} /></button><span className="action-divider" /><button className={`icon-button ${showInfo ? "icon-button--selected" : ""}`} title="Conversation details" onClick={() => setShowInfo(!showInfo)}><MoreVertical size={20} /></button></div>
        </header>
        {apiError && <div className="api-warning"><span>{apiError}</span><button onClick={() => setApiError("")}><X size={15} /></button></div>}
        <div className="message-scroll">
          <div className="encryption-note"><LockKeyhole size={13} /><span>Messages are stored in your CHAT-ME account.</span></div>
          <div className="date-divider"><span>TODAY</span></div>
          {loadingChat ? <div className="loading-messages"><div className="spinner" /> Loading conversation…</div> : activeMessages.map((msg, index) => {
            const own = Boolean(msg.own || msg.sender === me?.userName || msg.sender === "you");
            return <div key={msg.id || index} className={`message-line ${own ? "message-line--own" : ""}`}>
              <span className="message-avatar"><Avatar name={own ? me?.displayName || me?.userName || "You" : active.displayName || active.userName} color={own ? "mint" : active.color} /></span>
              <div className="message-bubble"><p>{msg.body}</p><span className="message-meta">{msg.sentAt || timeLabel(msg.createdAt) || ""}{own && <CheckCheck size={14} className="message-read" />}</span></div>
            </div>;
          })}
          {activeMessages.length === 0 && !loadingChat && <div className="empty-conversation"><span className="empty-conversation__icon"><MessageCircle size={24} /></span><h3>Start with a hello</h3><p>Your conversation with {active.displayName || active.userName} starts right here.</p><button onClick={() => { setMessage("Hey! 👋"); inputRef.current?.focus(); }}>Say hello <span>→</span></button></div>}
          <div ref={bottomRef} />
        </div>
        <div className="composer-wrap">
          <div className="composer-hint"><Sparkles size={13} /><span>A little kindness goes a long way.</span><span className="composer-hint__right">ENTER TO SEND</span></div>
          <form className="composer" onSubmit={sendMessage}>
            <div className="composer__tools"><button type="button" className="icon-button" title="Emoji" onClick={() => setMessage((old) => old + " 😊")}><Smile size={21} /></button><button type="button" className={`icon-button ${showAttach ? "icon-button--selected" : ""}`} title="Attach" onClick={() => setShowAttach(!showAttach)}><Paperclip size={20} /></button></div>
            {showAttach && <div className="attach-popover"><button type="button" onClick={() => { setToast("File sharing will be available in a future update."); setShowAttach(false); }}><FileText size={18} /> Document</button><button type="button" onClick={() => { setToast("Image sharing will be available in a future update."); setShowAttach(false); }}><ImageIcon size={18} /> Photos</button></div>}
            <textarea value={message} onChange={(e) => setMessage(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); sendMessage(e); } }} placeholder={`Message ${active.displayName || active.userName}…`} rows={1} aria-label="Message" />
            <button type="submit" className={`send-button ${message.trim() ? "send-button--ready" : ""}`} disabled={!message.trim()} aria-label="Send message"><Send size={18} /></button>
          </form>
          <p className="composer-disclaimer">Keep it kind. Keep it you. <span>CHAT-ME <i>✳</i></span></p>
        </div>
      </> : <div className="welcome-pane"><Brand /><span className="welcome-pane__illustration"><MessageCircle size={42} /><Sparkles size={20} /></span><h2>Your next good conversation<br /><em>starts here.</em></h2><p>Choose a conversation or find someone new to say hello to.</p><button className="primary-button" onClick={() => { setSearchMode("people"); inputRef.current?.focus(); }}>Find someone <span>→</span></button><small><LockKeyhole size={13} /> Personal conversations, one message at a time.</small></div>}
    </section>
    {showInfo && active && <aside className="info-panel"><div className="info-panel__header"><strong>Contact details</strong><button className="icon-button" onClick={() => setShowInfo(false)}><X size={18} /></button></div><div className="info-panel__person"><Avatar name={active.displayName || active.userName} color={active.color} large /><h3>{active.displayName || active.userName}</h3><p>@{active.userName}</p><span className="contact-badge"><i className={active.online ? "status-dot" : "status-dot status-dot--off"} />{active.online ? "Online now" : "CHAT-ME contact"}</span></div><div className="info-panel__section"><span>ABOUT THIS CHAT</span><p>Just you and {active.displayName || active.userName}. Keep the conversation going.</p></div><div className="info-panel__option"><Bell size={17} /><span>Notifications</span><span className="option-value">On</span></div><div className="info-panel__option"><Search size={17} /><span>Search messages</span><ChevronDown size={15} /></div><div className="info-panel__foot"><ShieldCheck size={15} /> Your conversation, your space.</div></aside>}
    <div className="app-corner"><span className={`connection-indicator ${isDemo ? "connection-indicator--demo" : ""}`} />{isDemo ? "PREVIEW MODE" : "CONNECTED"}<span>·</span> CHAT-ME v1.0</div>
    {toast && <div className="toast"><Check size={16} />{toast}<button onClick={() => setToast("")}><X size={14} /></button></div>}
    <button className="logout-fab" title="Sign out" onClick={logout}><LogOut size={17} /></button>
  </main>;
}
export default App;
