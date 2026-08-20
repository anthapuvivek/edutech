CREATE TABLE login_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    email VARCHAR(255) NOT NULL,
    action VARCHAR(30) NOT NULL,  -- LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, PASSWORD_RESET, ACCOUNT_LOCKED
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_login_audit_user_id ON login_audit_logs(user_id);
CREATE INDEX idx_login_audit_email ON login_audit_logs(email);
CREATE INDEX idx_login_audit_created_at ON login_audit_logs(created_at);
