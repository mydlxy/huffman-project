package huffman;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author myd
 * @date 2022/12/25  1:38
 */

public class HuffmanCode {

    class HMNode{
        int val;
        HMNode left;
        HMNode right;
        boolean leaf;//是否叶子节点
        char str;

        public HMNode(int val){
            this.val = val;
        }
        public HMNode(int val,char str){
            this.val = val;
            leaf = true;
            this.str = str;
        }

        @Override
        public String toString() {
            return "val="+val+";char="+(str=='\0' ? '_':str);
        }
    }


    /**
     *
     * 统计字符使用次数
     * @param text
     * @return
     */
    private Map<Character,Integer> count(String text){
        Map<Character,Integer> map = new HashMap<>();
        for (int i = 0; i <text.length() ; i++) {
            char t = text.charAt(i);
            if(map.containsKey(t)){
                map.put(t,map.get(t)+1);
            }else{
                map.put(t,1);
            }
        }
        return map;
    }

    /**
     *
     * 构建Huffman树
     * @param text
     * @return
     */
    private HMNode buildHuffManTree(String text){
        Map<Character,Integer> map = count(text);
        List<HMNode> nodes = new ArrayList<>(map.size()<<1);
        map.forEach((key,val)->nodes.add(new HMNode(val,key)));
        nodes.sort((n1, n2)-> n1.val-n2.val);
        int start = 0;
        while(nodes.size()-2>=start){
            HMNode root  = new HMNode(nodes.get(start).val+nodes.get(start+1).val);
            root.left = nodes.get(start);
            root.right = nodes.get(start+1);
            nodes.add(root);
            nodes.sort((n1, n2)-> n1.val-n2.val);
            start+=2;
        }
        return nodes.get(start);
    }

    static int ARRAY_SIZE = 1024;
    static int BIT_CELL_LENGTH = 64;
    private long[] convertBits(String code){
        int len = code.length() ;
        int codeLength = len% BIT_CELL_LENGTH ==0 ? len/BIT_CELL_LENGTH:len/BIT_CELL_LENGTH+1;
        int index =1;
        int offset = BIT_CELL_LENGTH-1;
        long[] bitCodes = new long[codeLength+1];
        char[] codes = code.toCharArray();
        for (int i = 0; i < codes.length; i++) {
            long bitCode = bitCodes[index];
            long c = codes[i]-'0';
            bitCode |= (c<<offset);
            bitCodes[index]=bitCode;
            if(offset == 0){
                offset = BIT_CELL_LENGTH-1;
                index += 1;
            }else{
                offset -= 1;
            }

        }
        bitCodes[0]=(long)len;//第一个数字是用来记录编码的长度；
        return bitCodes;
    }

    /**
     *
     * 将Huffman树转换成二进制码
     * 二进制码用数组表示
     * @param root
     * @param huffmanCode
     * @param code
     */
    private void bitCode(HMNode root,Map<Character,long[]> huffmanCode,String code){

        if(root.leaf){
            System.out.println(root.str+ " -> " + code);
            huffmanCode.put(root.str,convertBits(code));
            return;
        }
        if(root.left != null){
            bitCode(root.left,huffmanCode,code+"0");
        }
        if(root.right != null){
            bitCode(root.right,huffmanCode,code+"1");
        }

    }

    class EncodeInfo{
        List<List<Long>> encodes = new ArrayList<>();
        int lastLength = 0;//encodes最后一个list的最后一个long元素中，编码占几个
        HMNode root;//hu
        public EncodeInfo(HMNode root){
            List<Long> code = new ArrayList<>(ARRAY_SIZE);
            code.add(0L);
            encodes.add(code);
            this.root = root;
        }
    }

    /**
     *
     * 压缩
     * @param text
     * @param info
     */
    public void encode(String text,EncodeInfo info){
        Map<Character,long[]>code = new HashMap<>();
        bitCode(info.root,code,"");

        for (char c : text.toCharArray()) {
            addBitCode(info,code.get(c));
        }
    }


    private void addBitCode(EncodeInfo info ,long[] bits){
        List<List<Long>> encodes = info.encodes;
        List<Long> codes = encodes.get(encodes.size()-1);//获取到最后一个List;
        //得到二进制码数组最后一个元素的有效bit数；
        long last = bits[0]%BIT_CELL_LENGTH == 0 ? BIT_CELL_LENGTH : bits[0] % BIT_CELL_LENGTH;
        for (int i = 1; i < bits.length-1; i++) {
            long b = bits[i];
            int size = codes.size();
            long val = codes.get(size-1) ;
            val |= (b >>> info.lastLength);//前半部分
            codes.set(size-1,val);
            long after = b << (BIT_CELL_LENGTH-info.lastLength);//后半部分
            codes = addLast(info,after);
            info.lastLength = info.lastLength == 0 ? 0: BIT_CELL_LENGTH-info.lastLength;
        }
        long lastBits = bits[bits.length-1];
        long val = codes.get(codes.size()-1);
        val |= (lastBits >>> info.lastLength);
        codes.set(codes.size()-1,val);
        if(last + info.lastLength < BIT_CELL_LENGTH){
            info.lastLength += last;
        }else if(last + info.lastLength == BIT_CELL_LENGTH){
            info.lastLength =0;
            addLast(info,0L);
        }else{
            lastBits<<= (BIT_CELL_LENGTH-info.lastLength);
            addLast(info,lastBits);
            info.lastLength +=(last-BIT_CELL_LENGTH);
        }
    }


    private List<Long> addLast(EncodeInfo info,long val){
        List<List<Long>> encodes = info.encodes;
        List<Long> codes = encodes.get(encodes.size() - 1);
        int size = codes.size();
        if(size == ARRAY_SIZE){
            codes = new ArrayList<>(ARRAY_SIZE);
            encodes.add(codes);
        }
        codes.add(val);
        return codes;
    }


    /**
     *
     * 解压
     * @param info
     * @return
     */
    public String decode(EncodeInfo info){
        List<List<Long>> encodes = info.encodes;
        int endListIndex = encodes.size()-1;
        int endElementIndex = encodes.get(endListIndex).size()-1;
        HMNode cur = info.root;
        for (int i = 0; i < encodes.size(); i++) {
            List<Long> codes = encodes.get(i);
            for (int j = 0; j < codes.size(); j++) {
                if(i == endListIndex && j == endElementIndex){
                    cur = parse(cur,codes.get(j),info.lastLength,info);
                }else{
                    cur = parse(cur,codes.get(j),BIT_CELL_LENGTH,info);
                }
            }
        }

        return null;
    }

    /**
     * @param cur
     * @param bits
     * @param len long型变量的有效bit长度；
     * @param info
     * @return
     */
    private HMNode parse(HMNode cur,long bits,int len,EncodeInfo info){
        long curBit = 0;
        while(cur != null && len > 0){
            curBit = (bits>>>(BIT_CELL_LENGTH-1));
            if(curBit == 1 ){
                cur = cur.right;
            }else{
                cur = cur.left;
            }
            bits<<=1;
            len--;
            if(cur.leaf){
                System.out.print(cur.str);
                if(len ==0)
                    return null;
                else
                    break;
            }

        }
        if(len == 0)return cur;
        return parse(cur=info.root,bits,len,info);
    }





    @Test
    public void test(){
        String text = "aaabbbbbccccccddddee";
        HMNode root =buildHuffManTree(text);
        Map<Character,long[]>code = new HashMap<>();
        bitCode(root,code,"");
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++");
        code.forEach((key,val)->{
            long codeLen = val[0];
            System.out.print(key+"编码长度："+codeLen +"\tcode: ");
            for (int i = 1; i < val.length-1; i++) {
                System.out.print(" "+val[i]);
            }
            System.out.print(" "+(val[val.length-1]>>>(64-codeLen)));
            System.out.println("\n");
        });

    }
    @Test
    public void testDecode(){

        String text = "急急急uu  ,.;/.'*&^%$广东福建的好的的德国费尔法的的发夫算法发生过和323434234123吧vDVD产生的";
        int beforeEncode = text.getBytes().length;
        System.out.println("编码前：beforeEncode:"+beforeEncode +";");
        HMNode root =buildHuffManTree(text);
        EncodeInfo info =  new EncodeInfo(root);
        encode(text,info);

        decode(info);
        System.out.println("\n+++++++++++++++");
        System.out.println(text);

        info.encodes.forEach(x->{
            for (Long aLong : x) {
                System.out.println(aLong);
            }
        });
        System.out.println("最后一个long的有效bit个数："+info.lastLength);

    }

    @Test
    public void testEncode(){
        String text = " sfshnhGGHMBDV打发士大夫SASDSDW,.*&^%$#[]_+{};,.XCCCCC地方法规";
        int beforeEncode = text.getBytes().length;
        System.out.println("编码前：beforeEncode:"+beforeEncode +";");
        HMNode root =buildHuffManTree(text);
        EncodeInfo info =  new EncodeInfo(root);
        encode(text,info);
        info.encodes.forEach(x->{
            for (Long aLong : x) {
                System.out.println(aLong);
            }
        });

        System.out.println("最后一个long的有效bit个数："+info.lastLength);

    }



}
