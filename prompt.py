import os

def generate_java_prompt(directory,suffix):
    """
    递归遍历目录，收集所有Java文件内容并生成prompt字符串
    
    Args:
        directory (str): 要遍历的根目录路径
        
    Returns:
        str: 包含所有指定文件内容的prompt字符串
    """
    prompt = ""
    
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(suffix):
                # 获取文件的相对路径
                rel_path = os.path.relpath(os.path.join(root, file), directory)
                
                # 添加文件分隔注释
                prompt += f"\n\n// ====== FILE: {rel_path} ======\n\n"
                
                # 读取文件内容并添加到prompt
                try:
                    with open(os.path.join(root, file), 'r', encoding='utf-8') as f:
                        prompt += f.read()
                except UnicodeDecodeError:
                    # 如果UTF-8解码失败，尝试其他编码
                    try:
                        with open(os.path.join(root, file), 'r', encoding='latin-1') as f:
                            prompt += f.read()
                    except Exception as e:
                        prompt += f"// ERROR reading file: {str(e)}\n"
                except Exception as e:
                    prompt += f"// ERROR reading file: {str(e)}\n"
    
    return prompt

if __name__ == "__main__":
    # 指定要扫描的目录（可以是相对或绝对路径）
    target_directory = input("请输入要扫描的目录路径: ").strip()
    suffix = input("请输入要扫描的文件后缀名（如.java .txt .md 等）： ").strip()
    if not os.path.isdir(target_directory):
        print("错误: 指定的路径不是目录或不存在")
    else:
        prompt = generate_java_prompt(target_directory,suffix)
        
        # 将结果保存到文件
        output_file = suffix + "prompt.txt"
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(prompt)
        
        print(f"Prompt已生成并保存到 {output_file}")
        print(f"总字符数: {len(prompt)}")