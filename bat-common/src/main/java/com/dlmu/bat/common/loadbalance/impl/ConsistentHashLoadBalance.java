package com.dlmu.bat.common.loadbalance.impl;

import com.dlmu.bat.common.loadbalance.AbstractLoadBalance;
import com.dlmu.bat.common.loadbalance.InvokerContext;
import com.dlmu.bat.common.loadbalance.Node;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author heipacker
 * @param <T>
 */
public class ConsistentHashLoadBalance<T extends Node> extends AbstractLoadBalance<T> {

	private volatile ConsistentHashSelector<?> selector;
	
    private static final ThreadLocal<MessageDigest> MD5_THREADLOCAL = new ThreadLocal<MessageDigest>() {
        @Override
        protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    };
	
	@Override
	public T doSelect(List<?> sources, InvokerContext context) {
		 int identityHashCode = System.identityHashCode(sources);
	     if (selector == null || selector.getIdentityHashCode() != identityHashCode) {
	            selector = new ConsistentHashSelector<T>((List<T>) sources, identityHashCode, 160);
	     }
	     return (T) selector.select(context);
	}
	
	private static final class ConsistentHashSelector<T extends Node> {

        private final TreeMap<Long, T> virtualSources;
        
        private final int identityHashCode;

        public ConsistentHashSelector(List<T> sources, int identityHashCode, int hashNodes) {
            this.virtualSources = new TreeMap<Long, T>();
            this.identityHashCode = identityHashCode;
            for (T source : sources) {
                for (int i = 0; i < hashNodes / 4; i++) {
                    byte[] digest = md5(source.id() + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualSources.put(m, source);
                    }
                }
            }
        }

        public int getIdentityHashCode() {
            return identityHashCode;
        }

        public T select(InvokerContext context) {
            String key = context.id();
            byte[] digest = md5(key);
            return sekectForKey(hash(digest, 0));
        }

        private T sekectForKey(long hash) {
            T invoker;
            Long key = hash;

            if (!virtualSources.containsKey(key)) {
                SortedMap<Long, T> tailMap = virtualSources.tailMap(key);
                if (tailMap.isEmpty()) {
                    key = virtualSources.firstKey();
                } else {
                    key = tailMap.firstKey();
                }
            }
            invoker = virtualSources.get(key);
            return invoker;
        }

        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8) 
                    | (digest[(number * 4)] & 0xFF))
                    & 0xFFFFFFFFL;
        }

        private byte[] md5(String value) {
            MessageDigest md5 = MD5_THREADLOCAL.get();
            md5.reset();
            byte[] bytes;
            try {
                bytes = value.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            md5.update(bytes);
            return md5.digest();
        }

    }

}


