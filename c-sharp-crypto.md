Here are the custom helper and crypto classes from the C# project.
These must be converted to their KMP equivalents and placed in the crypto folder (next to Crc32c)

These classes internally use dotnet v9 crypto APIs and BouncyCastle APIs, so reference these when in doubt.
For crypto specific code prefer using already installed library from cryptography-kotlin https://github.com/whyoleg/cryptography-kotlin
If "cryptography-kotlin" doesn't cover it, call Kotlin APIs or native Android/iOS APIs.

Make sure everything is fully implemented. Do not leave and TODOs (unless they are in an already existing comment) and do not leave any place holders.

Ask if in doubt, don't guess!

## SensitiveByteArray
```
[DebuggerDisplay("Key={string.Join(\"-\", _key)}")]
public sealed class SensitiveByteArray: IDisposable, IGenericCloneable<SensitiveByteArray>
{
    [JsonIgnore] private byte[] _key;

    private SensitiveByteArray()
    {
    }

    public SensitiveByteArray(byte[] data)
    {
        SetKey(data);
    }

    public SensitiveByteArray(string data64)
    {
        SetKey(Convert.FromBase64String(data64));
    }

    private SensitiveByteArray(SensitiveByteArray other)
    {
        if (other._key == null)
        {
            _key = null;
        }
        else
        {
            SetKey(other._key);
        }
    }

    public void Dispose()
    {
        Wipe();
    }

    public SensitiveByteArray Clone()
    {
        return new SensitiveByteArray(this);
    }

    public void Wipe()
    {
        if (_key != null)
        {
            ByteArrayUtil.WipeByteArray(_key);
        }
        _key = null;
    }

    public void SetKey(byte[] data)
    {
        if (data == null)
        {
            throw new OdinSystemException("Can't assign a null key");
        }
        if (data.Length < 1)
        {
            throw new OdinSystemException("Can't set an empty key");
        }

        Wipe();

        _key = new byte[data.Length];
        Array.Copy(data, _key, data.Length);
    }

    public byte[] GetKey()
    {
        if (_key == null)
        {
            throw new OdinSystemException("No key set");
        }
        return _key;
    }

    public bool IsEmpty()
    {
        return _key == null;
    }

    public bool IsSet()
    {
        return _key != null;
    }
}
```

## UnixTimeUtc
```
[JsonConverter(typeof(UnixTimeUtcConverter))]
[DebuggerDisplay("dt={System.DateTimeOffset.FromUnixTimeMilliseconds(_milliseconds).ToString(\"yyyy-MM-dd HH:mm:ss.fff\")}")]
public readonly struct UnixTimeUtc : IGenericCloneable<UnixTimeUtc>, IEquatable<UnixTimeUtc>
{
    public static readonly UnixTimeUtc ZeroTime = new UnixTimeUtc(0);

    public UnixTimeUtc()
    {
        _milliseconds = (Int64)DateTime.UtcNow.Subtract(DateTime.UnixEpoch).TotalMilliseconds;
    }

    public UnixTimeUtc(Int64 milliseconds)
    {
        _milliseconds = milliseconds;
    }

    public UnixTimeUtc(UnixTimeUtc ut)
    {
        _milliseconds = ut.milliseconds;
    }

    public UnixTimeUtc(Instant nodaTime)
    {
        _milliseconds = nodaTime.ToUnixTimeMilliseconds();
    }

    public UnixTimeUtc(DateTimeOffset dto)
    {
        _milliseconds = dto.ToUnixTimeMilliseconds();
    }

    public UnixTimeUtc Clone()
    {
        return new UnixTimeUtc(_milliseconds);
    }

    // Define cast to Int64
    public static implicit operator UnixTimeUtc(Int64 milliseconds)
    {
        return new UnixTimeUtc(milliseconds);
    }

    public static explicit operator Int64(UnixTimeUtc ut)
    {
        return ut.milliseconds;
    }


    /// <summary>
    /// Returns a new UnixTimeUtc object with the seconds added.
    /// </summary>
    /// <param name="s">Seconds</param>
    public UnixTimeUtc AddSeconds(Int64 s)
    {
        return new UnixTimeUtc((Int64)(((Int64)_milliseconds) + (s * 1000)));
    }

    /// <summary>
    /// Returns a new UnixTimeUtc object with the minutes added.
    /// </summary>
    /// <param name="m">Minutes</param>
    public UnixTimeUtc AddMinutes(Int64 m)
    {
        return new UnixTimeUtc((Int64)(((Int64)_milliseconds) + (m * 60 * 1000)));
    }

    /// <summary>
    /// Returns a new UnixTimeUtc object with the hours added.
    /// </summary>
    /// <param name="h">Hours</param>
    public UnixTimeUtc AddHours(Int64 h)
    {
        return new UnixTimeUtc((Int64)(((Int64)_milliseconds) + (h * 60 * 60 * 1000)));
    }


    /// <summary>
    /// Returns a new UnixTimeUtc object with the hours added.
    /// </summary>
    /// <param name="d">Days</param>
    public UnixTimeUtc AddDays(Int64 d)
    {
        return new UnixTimeUtc((Int64)(((Int64)_milliseconds) + (d * 24 * 60 * 60 * 1000)));
    }

    /// <summary>
    /// Returns a new UnixTimeUtc object with the milliseconds added.
    /// </summary>
    /// <param name="ms"></param>
    public UnixTimeUtc AddMilliseconds(Int64 ms)
    {
        return new UnixTimeUtc((Int64)(((Int64)_milliseconds) + ms));
    }

    public static UnixTimeUtc Now()
    {
        return new UnixTimeUtc();
    }

    public static implicit operator Instant(UnixTimeUtc ms)
    {
        return Instant.FromUnixTimeMilliseconds(ms.milliseconds);
    }

    public static explicit operator UnixTimeUtc(Instant nodaTime)
    {
        return new UnixTimeUtc(nodaTime.ToUnixTimeMilliseconds());
    }

    public DateTime ToDateTime()
    {
        DateTime unixEpoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
        DateTime dateTime = unixEpoch.AddMilliseconds(this.milliseconds);
        return dateTime;
    }
    
    public DateTimeOffset ToDateTimeOffset()
    {
        return DateTimeOffset.FromUnixTimeMilliseconds(milliseconds);
    }

    public static UnixTimeUtc FromDateTime(DateTime dateTime)
    {
        DateTime unixEpoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
        long millisecondsSinceEpoch = (long)(dateTime - unixEpoch).TotalMilliseconds;
        return new UnixTimeUtc(millisecondsSinceEpoch);
    }
    
    public static UnixTimeUtc FromDateTimeOffset(DateTimeOffset dateTime)
    {
        DateTime unixEpoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
        long millisecondsSinceEpoch = (long)(dateTime - unixEpoch).TotalMilliseconds;
        return new UnixTimeUtc(millisecondsSinceEpoch);
    }
   

    public bool IsBetween(UnixTimeUtc start, UnixTimeUtc end, bool inclusive = true)
    {
        if (inclusive)
        {
            return this >= start && this <= end;
        }

        return this > start && this < end;
    }

    public static TimeSpan operator -(UnixTimeUtc left, UnixTimeUtc right)
    {
        return TimeSpan.FromMilliseconds(left.milliseconds - right.milliseconds);
    }
    
    public static bool operator ==(UnixTimeUtc left, UnixTimeUtc right)
    {
        return left.milliseconds == right.milliseconds;
    }

    public static bool operator !=(UnixTimeUtc left, UnixTimeUtc right)
    {
        return left.milliseconds != right.milliseconds;
    }

    public static bool operator >(UnixTimeUtc left, UnixTimeUtc right)
    {
        return left.milliseconds > right.milliseconds;
    }

    public static bool operator >=(UnixTimeUtc left, UnixTimeUtc right)
    {
        return left.milliseconds >= right.milliseconds;
    }

    public static bool operator <=(UnixTimeUtc left, UnixTimeUtc right)
    {
        return left.milliseconds <= right.milliseconds;
    }

    public static bool operator <(UnixTimeUtc left, UnixTimeUtc right)
    {
        return left.milliseconds < right.milliseconds;
    }
    public bool Equals(UnixTimeUtc other)
    {
        return this._milliseconds == other._milliseconds;
    }

    public override bool Equals(object value)
    {
        if (!(value is UnixTimeUtc))
            return false;

        return ((UnixTimeUtc)value).milliseconds == this.milliseconds;
    }

    public override int GetHashCode()
    {
        return this.milliseconds.GetHashCode();
    }

    public Int64 milliseconds
    {
        get { return _milliseconds; }
    }

    public Int64 seconds
    {
        get { return _milliseconds / 1000; }
    }

    /// <summary>
    /// TODO ISO name
    /// TODO I don't think I need to convert to DateTime first
    /// Outputs time as ISO ... "yyyy-MM-ddTHH:mm:ssZ"
    /// </summary>
    /// <returns>ISO ... string</returns>
    public string Iso9441()
    {
        return ToDateTime().ToString("yyyy-MM-ddTHH:mm:ssZ");
    }

    public override string ToString()
    {
        return _milliseconds.ToString();
    }

    private readonly Int64 _milliseconds;
}

```

## AesCbc
```
public static class AesCbc
{
    private static byte[] PerformCryptography(byte[] data, ICryptoTransform cryptoTransform)
    {
        using var ms = new MemoryStream();
        using var cryptoStream = new CryptoStream(ms, cryptoTransform, CryptoStreamMode.Write);
        cryptoStream.Write(data, 0, data.Length);
        cryptoStream.FlushFinalBlock();

        return ms.ToArray();
    }

    /// <summary>
    /// Do not use this function unless you specifically need to reencrypt with the same IV.
    /// This is only needed when we transform the header.
    /// </summary>
    /// <param name="data"></param>
    /// <param name="key"></param>
    /// <param name="iv"></param>
    /// <returns></returns>
    public static byte[] Encrypt(byte[] data, SensitiveByteArray key, byte[] iv)
    {
        return Encrypt(data, key.GetKey(), iv);
    }

    public static byte[] Encrypt(byte[] data, byte[] key, byte[] iv)
    {
        if (data == null)
        {
            throw new ArgumentNullException(nameof(data), "Data cannot be null.");
        }
        if (key == null)
        {
            throw new ArgumentNullException(nameof(key), "Key cannot be null.");
        }
        if (iv == null)
        {
            throw new ArgumentNullException(nameof(iv), "IV cannot be null.");
        }

        using var aesAlg = Aes.Create();
        aesAlg.Key = key;
        aesAlg.IV = iv;
        aesAlg.Mode = CipherMode.CBC;

        using var encryptor = aesAlg.CreateEncryptor(aesAlg.Key, aesAlg.IV);
        return PerformCryptography(data, encryptor);
    }

    //public static (byte[] IV, byte[] ciphertext) Encrypt(byte[] data, byte[] Key)
    public static (byte[] IV, byte[] ciphertext) Encrypt(byte[] data, SensitiveByteArray key)
    {
        if (data == null)
        {
            throw new ArgumentNullException(nameof(data), "Data cannot be null.");
        }
        if (key == null)
        {
            throw new ArgumentNullException(nameof(key), "Key cannot be null.");
        }

        using var aesAlg = Aes.Create();
        aesAlg.Key = key.GetKey();

        aesAlg.GenerateIV();
        var iv = aesAlg.IV;

        aesAlg.Mode = CipherMode.CBC;

        using var encryptor = aesAlg.CreateEncryptor(aesAlg.Key, aesAlg.IV);
        return (iv, PerformCryptography(data, encryptor));
    }

    public static byte[] Decrypt(byte[] cipherText, SensitiveByteArray key, byte[] iv)
    {
        return Decrypt(cipherText, key.GetKey(), iv);
    }

    public static byte[] Decrypt(byte[] cipherText, byte[] key, byte[] iv)
    {
        if (cipherText == null)
        {
            throw new ArgumentNullException(nameof(cipherText), "CipherText cannot be null.");
        }
        if (key == null)
        {
            throw new ArgumentNullException(nameof(key), "Key cannot be null.");
        }
        if (iv == null)
        {
            throw new ArgumentNullException(nameof(iv), "IV cannot be null.");
        }

        // Create an Aes object
        // with the specified key and IV.
        using var aesAlg = Aes.Create();
        aesAlg.Key = key;
        aesAlg.IV = iv;
        aesAlg.Mode = CipherMode.CBC;

        using var decryptor = aesAlg.CreateDecryptor(aesAlg.Key, aesAlg.IV);
        return PerformCryptography(cipherText, decryptor);
    }
}
```

## HashUtil
```
public static class HashUtil
{
    public const string SHA256Algorithm = "SHA-256";

    public static byte[] Hkdf(byte[] sharedEccSecret, byte[] salt, int outputKeySize)
    {
        if (sharedEccSecret == null)
            throw new ArgumentNullException(nameof(sharedEccSecret));

        if (salt == null)
            throw new ArgumentNullException(nameof(salt));

        if (outputKeySize < 16)
            throw new ArgumentException("Output key size cannot be less than 16", nameof(outputKeySize));

        // Create an instance of HKDFBytesGenerator with SHA-256
        HkdfBytesGenerator hkdf = new HkdfBytesGenerator(new Sha256Digest());

        // Initialize the generator
        hkdf.Init(new HkdfParameters(sharedEccSecret, salt, null));

        // Create a byte array for the output key
        byte[] outputKey = new byte[outputKeySize];

        // Generate the key
        hkdf.GenerateBytes(outputKey, 0, outputKey.Length);

        return outputKey;
    }

    public static (byte[], Int64) StreamSHA256(Stream inputStream, byte[] optionalNonce = null)
    {
        using (var hasher = SHA256.Create())
        {
            // if nonce is provided, compute hash of nonce first
            if (optionalNonce != null)
            {
                hasher.TransformBlock(optionalNonce, 0, optionalNonce.Length, null, 0);
            }

            Int64 streamLength = 0;

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.Read(buffer, 0, buffer.Length)) != 0)
            {
                hasher.TransformBlock(buffer, 0, bytesRead, null, 0);
                streamLength += bytesRead;
            }

            // finalize the hash computation
            hasher.TransformFinalBlock(buffer, 0, 0);

            return (hasher.Hash, streamLength);
        }
    }
}
```

## Base64UrlEncoder
```
public static class Base64UrlEncoder
{
    public static string Encode(byte[] input)
    {
        return WebEncoders.Base64UrlEncode(input);
    }

    //

    public static string Encode(string input)
    {
        return Encode(input.ToUtf8ByteArray());
    }

    //

    public static byte[] Decode(string input)
    {
        return WebEncoders.Base64UrlDecode(input);
    }

    //

    public static string DecodeString(string input)
    {
        return Decode(input).ToStringFromUtf8Bytes();
    }

    //
}
```

## ByteArrayUtil
```
public static class ByteArrayUtil
{
    public static byte[] UInt32ToBytes(UInt32 i)
    {
        return new byte[] { (byte)(i >> 24 & 0xFF), (byte)(i >> 16 & 0xFF), (byte)(i >> 8 & 0xFF), (byte)(i & 0xFF) };
    }

    public static byte[] UInt64ToBytes(UInt64 i)
    {
        return new byte[] { (byte)(i >> 56 & 0xFF), (byte)(i >> 48 & 0xFF), (byte)(i >> 40 & 0xFF), (byte)(i >> 32 & 0xFF), (byte)(i >> 24 & 0xFF), (byte)(i >> 16 & 0xFF), (byte)(i >> 8 & 0xFF), (byte)(i & 0xFF) };
    }

    public static byte[] Int8ToBytes(sbyte i)
    {
        return new byte[] { (byte)i };
    }

    public static byte[] Int16ToBytes(Int16 i)
    {
        return new byte[] { (byte)(i >> 8 & 0xFF), (byte)(i & 0xFF) };
    }

    public static byte[] Int32ToBytes(Int32 i)
    {
        return new byte[] { (byte)(i >> 24 & 0xFF), (byte)(i >> 16 & 0xFF), (byte)(i >> 8 & 0xFF), (byte)(i & 0xFF) };
    }

    public static byte[] Int64ToBytes(Int64 i)
    {
        return new byte[] { (byte)(i >> 56 & 0xFF), (byte)(i >> 48 & 0xFF), (byte)(i >> 40 & 0xFF), (byte)(i >> 32 & 0xFF), (byte)(i >> 24 & 0xFF), (byte)(i >> 16 & 0xFF), (byte)(i >> 8 & 0xFF), (byte)(i & 0xFF) };
    }

    public static Int64 BytesToInt64(byte[] bytes)
    {
        if (bytes == null || bytes.Length != 8)
            throw new ArgumentException("Input byte array must have exactly 8 elements.");

        return ((Int64)bytes[0] << 56)
            | ((Int64)bytes[1] << 48)
            | ((Int64)bytes[2] << 40)
            | ((Int64)bytes[3] << 32)
            | ((Int64)bytes[4] << 24)
            | ((Int64)bytes[5] << 16)
            | ((Int64)bytes[6] << 8)
            | (Int64)bytes[7];
    }


    public static Int32 BytesToInt32(byte[] bytes)
    {
        if (bytes == null || bytes.Length != 4)
            throw new ArgumentException("Input byte array must have exactly 4 elements.");

        return (bytes[0] << 24)
            | (bytes[1] << 16)
            | (bytes[2] << 8)
            | bytes[3];
    }


    public static Int16 BytesToInt16(byte[] bytes)
    {
        if (bytes == null || bytes.Length != 2)
            throw new ArgumentException("Input byte array must have exactly 2 elements.");

        return (Int16)((bytes[0] << 8) | bytes[1]);
    }

    public static sbyte BytesToInt8(byte[] bytes)
    {
        if (bytes == null || bytes.Length != 1)
            throw new ArgumentException("Input byte array must have exactly 1 element.");

        return (sbyte)bytes[0];
    }

    /// <summary>
    /// Concatenates any number of byte arrays in parameter order
    /// </summary>
    /// <param name="arrays">Two or more byte arrays to concatenate</param>
    /// <returns>The concatenated byte array</returns>
    public static byte[] Combine(params byte[][] arrays) => arrays.SelectMany(a => a).ToArray();


    public static byte[][] Split(byte[] data, params int[] lengths)
    {
        if (data == null)
            throw new ArgumentNullException(nameof(data));
        if (lengths == null)
            throw new ArgumentNullException(nameof(lengths));

        int totalLengths = lengths.Sum();
        if (totalLengths != data.Length)
            throw new ArgumentException("The sum of lengths does not match the data length.");

        byte[][] result = new byte[lengths.Length][];
        int offset = 0;

        for (int i = 0; i < lengths.Length; i++)
        {
            result[i] = new byte[lengths[i]];
            Buffer.BlockCopy(data, offset, result[i], 0, lengths[i]);
            offset += lengths[i];
        }

        return result;
    }

    public static (byte[] part1, byte[] part2) Split(byte[] data, int len1, int len2)
    {
        var part1 = new byte[len1];
        var part2 = new byte[len2];

        Buffer.BlockCopy(data, 0, part1, 0, len1);
        Buffer.BlockCopy(data, len1, part2, 0, len2);

        return (part1, part2);
    }

    public static (byte[] part1, byte[] part2, byte[] part3) Split(byte[] data, int len1, int len2, int len3)
    {
        var part1 = new byte[len1];
        var part2 = new byte[len2];
        var part3 = new byte[len3];

        Buffer.BlockCopy(data, 0, part1, 0, len1);
        Buffer.BlockCopy(data, len1, part2, 0, len2);
        Buffer.BlockCopy(data, len1 + len2, part3, 0, len3);

        return (part1, part2, part3);
    }

    public static byte[] CalculateSHA256Hash(byte[] input)
    {
        using (SHA256 sha256Hash = SHA256.Create())
        {
            // ComputeHash - returns byte array  
            byte[] bytes = sha256Hash.ComputeHash(input);
            return bytes;
        }
    }

    public static Guid ReduceSHA256Hash(string input)
    {
        return new Guid(ReduceSHA256Hash(input.ToUtf8ByteArray()));
    }

    public static byte[] ReduceSHA256Hash(byte[] input)
    {
        var bytes = CalculateSHA256Hash(input);
        var half = bytes.Length / 2;
        var (part1, part2) = ByteArrayUtil.Split(bytes, half, half);
        var reducedBytes = ByteArrayUtil.EquiByteArrayXor(part1, part2);
        return reducedBytes;
    }

    public static string PrintByteArray(byte[] bytes)
    {
        if (bytes == null || bytes.Length == 0)
            return "new byte[] { }";

        return "new byte[] { " + string.Join(", ", bytes) + " }";
    }


    // Oh memset() oh memset().... I love memset()... Why write fancy for loops when
    // you can brutally use memset... I know the answer. But I still love memset(). 
    public static void WipeByteArray(byte[] b)
    {
        Array.Clear(b, 0, b.Length);
    }

    /// <summary>
    /// Returns a cryptographically strong random Guid
    /// </summary>
    /// <returns></returns>
    public static Guid GetRandomCryptoGuid()
    {
        return new Guid(GetRndByteArray(16));
    }

    /// <summary>
    /// Returns true if key is strong, false if it appears constructed or wrong
    /// </summary>
    /// <param name="data"></param>
    /// <returns></returns>
    public static bool IsStrongKey(byte[] data)
    {
        if (data == null || data.Length < 16)
            return false;

        int j = 0;

        // Keys like this are considered weak "nnnn mmmm oooo pppp"
        for (int i = 0; i < data.Length / 4; i++, j+=4)
        {
            if ((data[j] != data[j + 1]) ||
                (data[j] != data[j + 2]) ||
                (data[j] != data[j + 3]))
                return true;
        }

        if (data.Length % 4 != 0)
        {
            // If the key is an odd size then let's just see if the last
            // bytes are the same as the byte before
            for (int i = 0; i < data.Length % 4; i++)
            {
                if (data[j-1] != data[j + i])
                    return true;
            }

        }

        return false;
    }

    /// <summary>
    /// Generates a cryptographically safe (?) array of random bytes. To be used for XORing private keys
    /// </summary>
    /// <param name="nCount">Number of bytes (should be as long as data to XOR</param>
    /// <returns>Array of random bytes of the specified length</returns>
    public static byte[] GetRndByteArray(int nCount)
    {
        using (var rng = RandomNumberGenerator.Create())
        {
            byte[] byteArray = new byte[nCount];
            rng.GetBytes(byteArray);
            return byteArray;
        }
    }

    /// <summary>
    /// Check if two byte arrays of equal length are identical.
    /// </summary>
    /// <param name="ba1"></param>
    /// <param name="ba2"></param>
    /// <returns>True if identical, false otherwise</returns>
    public static bool EquiByteArrayCompare(byte[] ba1, byte[] ba2)
    {
        if (ba1 == null || ba2 == null)
            throw new ArgumentNullException("One or both byte arrays are null.");

        return ba1.Length == ba2.Length && ba1.SequenceEqual(ba2);
    }

    /// <summary>
    /// XOR the two byte arrays with each other. Requires the same length.
    /// </summary>
    /// <param name="ba1"></param>
    /// <param name="ba2"></param>
    /// <returns>The XOR'ed byte array</returns>
    public static byte[] EquiByteArrayXor(byte[] ba1, byte[] ba2)
    {
        if (ba1.Length != ba2.Length)
            throw new ArgumentException("Byte arrays are not the same length");

        byte[] ra = new byte[ba1.Length];
        int i = 0;
        while (i < ba1.Length)
        {
            ra[i] = (byte)(ba1[i] ^ ba2[i]);
            i++;
        }

        return ra;
    }

    // memcmp for two 16 byte arrays
    // 1 : b1 > b2; 0 equal; -1 : b1 < b2
    public static int muidcmp(byte[] b1, byte[] b2)
    {
        if ((b1 == null) || (b2 == null))
        {
            if (b1 == b2)
                return 0;
            else if (b1 == null)
                return -1;
            else
                return +1;
        }

        if ((b1.Length != 16) || (b2.Length != 16))
            throw new Exception("b1,b2 must be 16 bytes");

        for (int i = 0; i < 16; i++)
        {
            if (b1[i] == b2[i])
                continue;
            if (b1[i] > b2[i])
                return 1; // b1 larger than b2
            else
                return -1; // b2 larger than b1
        }

        return 0;
    }

    /// <summary>
    /// memcmp for two Guids.
    /// </summary>
    /// <param name="b1"></param>
    /// <param name="b2"></param>
    /// <returns>+1 : b1 > b2; 0 equal; -1 : b1 < b2</returns>
    public static int muidcmp(Guid? b1, Guid? b2)
    {
        return muidcmp(b1?.ToByteArray(), b2?.ToByteArray());
    }
}

```

## EccPublicKeyData and EccFullKeyData
```
using System;
using System.Collections.Generic;
using System.Text.Json;
using System.Text.Json.Serialization;
using Odin.Core.Cryptography.Crypto;
using Odin.Core.Time;
using Org.BouncyCastle.Asn1.Nist;
using Org.BouncyCastle.Asn1.Sec;
using Org.BouncyCastle.Asn1.X509;
using Org.BouncyCastle.Asn1.X9;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Agreement;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Math;
using Org.BouncyCastle.Math.EC;
using Org.BouncyCastle.Pkcs;
using Org.BouncyCastle.Security;
using Org.BouncyCastle.X509;

namespace Odin.Core.Cryptography.Data
{
    public enum EccKeySize
    {
        P256 = 0,
        P384 = 1
    }

    public class EccPublicKeyData
    {
        public static string[] eccSignatureAlgorithmNames = new string[2] { "SHA-256withECDSA", "SHA-384withECDSA" };
        public static string[] eccKeyTypeNames = new string[2] { "P-256", "P-384" };
        public static string[] eccCurveIdentifiers = new string[2] { "secp256r1", "secp384r1" };

        public byte[] publicKey { get; set; } // DER encoded public key

        public UInt32 crc32c { get; set; } // The CRC32C of the public key
        public UnixTimeUtc expiration { get; set; } // Time when this key expires

        public static EccPublicKeyData FromJwkPublicKey(string jwk, int hours = 1)
        {
            try
            {
                var jwkObject = JsonSerializer.Deserialize<Dictionary<string, string>>(jwk);

                if (jwkObject["kty"] != "EC")
                    throw new InvalidOperationException("Invalid key type, kty must be EC");

                string curveName = jwkObject["crv"];
                if ((curveName != "P-384") && (curveName != "P-256"))
                    throw new InvalidOperationException("Invalid curve, crv must be P-384 OR P-256");

                byte[] x = Base64UrlEncoder.Decode(jwkObject["x"]);
                byte[] y = Base64UrlEncoder.Decode(jwkObject["y"]);

                X9ECParameters x9ECParameters = NistNamedCurves.GetByName(curveName);
                ECCurve curve = x9ECParameters.Curve;
                ECPoint ecPoint = curve.CreatePoint(new BigInteger(1, x), new BigInteger(1, y));

                ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(ecPoint,
                    new ECDomainParameters(curve, x9ECParameters.G, x9ECParameters.N, x9ECParameters.H));

                SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfoFactory.CreateSubjectPublicKeyInfo(publicKeyParameters);
                byte[] derEncodedPublicKey = publicKeyInfo.GetDerEncoded();

                var publicKey = new EccPublicKeyData()
                {
                    publicKey = derEncodedPublicKey,
                    crc32c = KeyCRC(derEncodedPublicKey),
                    expiration = UnixTimeUtc.Now().AddSeconds(hours * 60 * 60)
                };

                return publicKey;
            }
            catch (FormatException)
            {
                throw new Exception("Invalid Jwk public key format");
            }
        }

        public static EccPublicKeyData FromJwkBase64UrlPublicKey(string jwkbase64Url, int hours = 1)
        {
            return FromJwkPublicKey(Base64UrlEncoder.DecodeString(jwkbase64Url), hours);
        }

        protected EccKeySize GetCurveEnum(ECCurve curve)
        {
            int bitLength = curve.Order.BitLength;

            if (bitLength == 384)
            {
                return EccKeySize.P384;
            }
            else if (bitLength == 256)
            {
                return EccKeySize.P256;
            }
            else
            {
                throw new Exception($"Unsupported ECC key size with bit length: {bitLength}");
            }
        }


        // Method to ensure byte array length
        private byte[] EnsureLength(byte[] bytes, int length)
        {
            if (bytes.Length >= length) return bytes;

            byte[] paddedBytes = new byte[length];
            Array.Copy(bytes, 0, paddedBytes, length - bytes.Length, bytes.Length);
            return paddedBytes;
        }


        public string PublicKeyJwk()
        {
            var publicKeyRestored = PublicKeyFactory.CreateKey(publicKey);
            ECPublicKeyParameters publicKeyParameters = (ECPublicKeyParameters)publicKeyRestored;

            // Extract the key parameters
            BigInteger x = publicKeyParameters.Q.AffineXCoord.ToBigInteger();
            BigInteger y = publicKeyParameters.Q.AffineYCoord.ToBigInteger();

            var curveSize = GetCurveEnum((ECCurve)publicKeyParameters.Parameters.Curve);

            int expectedBytes;
            if (curveSize == EccKeySize.P384)
                expectedBytes = 384 / 8;
            else
                expectedBytes = 256 / 8;

            var xBytes = EnsureLength(x.ToByteArrayUnsigned(), expectedBytes);
            var yBytes = EnsureLength(y.ToByteArrayUnsigned(), expectedBytes);

            string curveName = eccKeyTypeNames[(int)curveSize];

            // Create a JSON object to represent the JWK
            var jwk = new
            {
                kty = "EC",
                crv = curveName, // P-256 or P-384
                x = Base64UrlEncoder.Encode(xBytes),
                y = Base64UrlEncoder.Encode(yBytes)
            };

            var options = new JsonSerializerOptions
            {
                DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
                WriteIndented = false
            };

            string jwkJson = JsonSerializer.Serialize(jwk, options);

            return jwkJson;
        }

        public string PublicKeyJwkBase64Url()
        {
            return Base64UrlEncoder.Encode(PublicKeyJwk());
        }


        public static UInt32 KeyCRC(byte[] keyDerEncoded)
        {
            return CRC32C.CalculateCRC32C(0, keyDerEncoded);
        }

        public UInt32 KeyCRC()
        {
            return KeyCRC(publicKey);
        }

    }

    public class EccFullKeyData : EccPublicKeyData
    {
        private SensitiveByteArray _privateKey; // Cached decrypted private key, not stored

        public byte[] storedKey { get; set; } // The key as stored on disk encrypted with a secret key or constant

        public byte[] iv { get; set; } // Iv used for encrypting the storedKey and the masterCopy
        public byte[] keyHash { get; set; } // The hash of the encryption key

        public UnixTimeUtc
            createdTimeStamp
        {
            get;
            set;
        } // Time when this key was created, expiration is on the public key. Do NOT use a property or code will return a copy value.


        /// <summary>
        /// Use this constructor. Key is the encryption key used to encrypt the private key
        /// </summary>
        /// <param name="key">The key used to (AES) encrypt the private key</param>
        /// <param name="size"></param>
        /// <param name="hours">Lifespan of the key, required</param>
        /// <param name="minutes">Lifespan of the key, optional</param>
        /// <param name="seconds">Lifespan of the key, optional</param>
        public EccFullKeyData(SensitiveByteArray key, EccKeySize keySize, int hours, int minutes = 0, int seconds = 0)
        {
            // Generate an EC key with Bouncy Castle, curve secp384r1
            ECKeyPairGenerator generator = new ECKeyPairGenerator();
            X9ECParameters ecp = SecNamedCurves.GetByName(eccCurveIdentifiers[(int)keySize]);

            var domainParams = new ECDomainParameters(ecp.Curve, ecp.G, ecp.N, ecp.H, ecp.GetSeed());
            generator.Init(new ECKeyGenerationParameters(domainParams, new SecureRandom()));
            AsymmetricCipherKeyPair keys = generator.GenerateKeyPair();

            // Extract the public and the private keys
            var privateKeyInfo = PrivateKeyInfoFactory.CreatePrivateKeyInfo(keys.Private);
            var publicKeyInfo = SubjectPublicKeyInfoFactory.CreateSubjectPublicKeyInfo(keys.Public);

            // Save the DER encoded private and public keys in our own data structure
            createdTimeStamp = UnixTimeUtc.Now();
            expiration = createdTimeStamp;
            expiration = expiration.AddSeconds(hours * 3600 + minutes * 60 + seconds);
            if (expiration <= createdTimeStamp)
                throw new Exception("Expiration must be > 0");

            CreatePrivate(key, privateKeyInfo.GetDerEncoded()); // TODO: Can we cleanup the generated key?

            publicKey = publicKeyInfo.GetDerEncoded();
            crc32c = KeyCRC();
        }

        private void CreatePrivate(SensitiveByteArray key, byte[] fullDerKey)
        {
            iv = ByteArrayUtil.GetRndByteArray(16);
            keyHash = ByteArrayUtil.ReduceSHA256Hash(key.GetKey());
            _privateKey = new SensitiveByteArray(fullDerKey);
            storedKey = AesCbc.Encrypt(_privateKey.GetKey(), key, iv);
        }


        private SensitiveByteArray GetFullKey(SensitiveByteArray key)
        {
            if (ByteArrayUtil.EquiByteArrayCompare(keyHash, ByteArrayUtil.ReduceSHA256Hash(key.GetKey())) == false)
                throw new Exception("Incorrect key");

            if (_privateKey == null)
            {
                _privateKey = new SensitiveByteArray(AesCbc.Decrypt(storedKey, key, iv));
            }

            return _privateKey;
        }


        public SensitiveByteArray GetEcdhSharedSecret(SensitiveByteArray pwd, EccPublicKeyData remotePublicKey, byte[] randomSalt)
        {
            if (remotePublicKey == null)
                throw new ArgumentNullException(nameof(remotePublicKey));

            if (remotePublicKey.publicKey == null)
                throw new ArgumentNullException(nameof(remotePublicKey.publicKey));

            if (randomSalt == null)
                throw new ArgumentNullException(nameof(randomSalt));

            if (randomSalt.Length < 16)
                throw new ArgumentException("Salt must be at least 16 bytes");

            // Retrieve the private key from the secure storage
            var privateKeyBytes = GetFullKey(pwd).GetKey();
            var privateKeyParameters = (ECPrivateKeyParameters)PrivateKeyFactory.CreateKey(privateKeyBytes);

            // Construct the public key parameters from the provided data
            var publicKeyParameters = (ECPublicKeyParameters)PublicKeyFactory.CreateKey(remotePublicKey.publicKey);

            // Initialize ECDH basic agreement
            ECDHBasicAgreement ecdhUagree = new ECDHBasicAgreement();
            ecdhUagree.Init(privateKeyParameters);

            // Calculate the shared secret
            BigInteger sharedSecret = ecdhUagree.CalculateAgreement(publicKeyParameters);

            // Convert the shared secret to a byte array
            var sharedSecretBytes = sharedSecret.ToByteArrayUnsigned().ToSensitiveByteArray();

            // Apply HKDF to derive a symmetric key from the shared secret
            return HashUtil.Hkdf(sharedSecretBytes.GetKey(), randomSalt, 16).ToSensitiveByteArray();
        }

    }
}
```
