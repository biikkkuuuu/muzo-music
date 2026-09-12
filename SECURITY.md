# Security Policy

## Supported Versions

We release patches for security vulnerabilities in the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.2.x   | :white_check_mark: |
| > 1.2   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in Muzi Music, please report it responsibly:

1. **Do NOT** create a public GitHub issue
2. Report via GitHub Private Security Advisories: [biikkkuuuu/muzi-music/security/advisories](https://github.com/biikkkuuuu/muzi-music/security/advisories)
3. Include the following information:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Any suggested fixes

## Security Best Practices

### For Developers

- **Never commit sensitive files**: API keys, tokens, and credentials should never be committed to version control
- **Use environment variables**: Store sensitive configuration in environment variables or secure properties files
- **Regular updates**: Keep dependencies updated to patch security vulnerabilities
- **Code review**: All code changes should be reviewed before merging

### For Users

- **Download from official sources**: Only download APKs from official releases or trusted sources
- **Keep the app updated**: Install updates promptly to receive security patches
- **Review permissions**: Be aware of the permissions the app requests

## Sensitive Information

The following files contain sensitive information and should never be committed:

- `google-services.json` - Firebase configuration with API keys
- `local.properties` - Local development configuration
- `*.keystore` / `*.jks` - App signing keys
- `secrets.properties` - API keys and secrets

## Data Privacy

Muzi Music is committed to user privacy:

- **No personal data collection**: We don't collect personal information
- **Local storage**: User data is stored locally on the device
- **Open source**: All code is available for review

Thank you for helping keep Muzi Music secure!
