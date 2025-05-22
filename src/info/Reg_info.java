package info;
import java.io.Serializable;
import java.io.Serial;
public class Reg_info implements Serializable {
    @Serial
    private static final long serialVersionUID = 5425677582818600106L;
    private String username;
    private String password;
    private int reg_status;//当前注册状态，0为注册中，1为注册成功，2为注册失败(用户名错误)
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public void set_info(String username, String password, int reg_status) {
        this.username = username;
        this.password = password;
        this.reg_status = reg_status;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public int getReg_status() {
        return reg_status;
    }
    
    public void setReg_status(int reg_status) {
        this.reg_status = reg_status;
    }
}
