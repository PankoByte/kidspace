# Contributing

Thanks for looking at Kid Space. The code is public so parents and reviewers can check what the app does. It is not a community-developed project, and the [license](LICENSE) does not allow modified versions.

## What helps most

- Bug reports. Include your phone model, Android version, and the steps that lead to the problem.
- Reports of ways a child can leave Kid mode. Send these privately; see [SECURITY.md](SECURITY.md).
- Translation fixes. Open an issue with the language, the current text and your correction.
- Questions about what a piece of code does. If something looks suspicious, ask.

## Code changes

Open an issue before writing code. Pull requests that arrive without one are usually closed, because the license does not grant the right to modify the app and we need to agree on the change first.

If we do agree on a pull request:

- Keep it small and about one thing.
- Run `./gradlew testDebugUnitTest lintDebug` before you push. Both must pass.
- Add or update a unit test when you touch `GuardLogic`, `KidLogic` or `PinLogic`.
- Do not add the `INTERNET` permission, analytics, ads or any third-party SDK.
- Put user-facing text in `strings.xml`, and keep it short.

By submitting a pull request you confirm that you wrote the change, and you give PankoByte the right to use, modify and distribute it under any license, including the one in this repository.
