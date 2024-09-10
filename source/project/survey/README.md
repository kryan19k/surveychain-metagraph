# Survey Metagraph

This metagraph implements a decentralized survey system using Constellation Network's Data API. It allows users to create surveys, submit responses, and retrieve survey data securely.

## Features

- Create surveys with customizable questions
- Submit encrypted responses to surveys
- Retrieve survey data and encrypted responses
- Token rewards for survey participants
- Access control for survey creators

## Routes

The metagraph exposes the following API endpoints:

### 1. Get All Surveys

- **Endpoint**: `GET /surveys`
- **Description**: Retrieves a list of all surveys.
- **Response**: List of `SurveyResponse` objects.

### 2. Get Survey by ID

- **Endpoint**: `GET /surveys/{surveyId}`
- **Description**: Retrieves a specific survey by its ID.
- **Response**: `SurveyResponse` object.

### 3. Get Survey Responses

- **Endpoint**: `GET /surveys/{surveyId}/responses`
- **Description**: Retrieves all responses for a specific survey.
- **Response**: List of `Response` objects (encrypted).

### 4. Get Surveys by Address

- **Endpoint**: `GET /addresses/{address}/surveys`
- **Description**: Retrieves all surveys created by a specific address.
- **Response**: List of `SurveyResponse` objects.

### 5. Decrypt Survey Responses

- **Endpoint**: `POST /surveys/{surveyId}/decrypt`
- **Description**: Decrypts and retrieves responses for a survey (requires access key).
- **Request Body**: `DecryptResponses` object containing the access key.
- **Response**: `EncryptedResponsesResponse` object.

## Data Structures

### Survey

- `id`: String
- `creator`: Address
- `title`: String
- `description`: String
- `questions`: List of Question objects
- `tokenReward`: String
- `endTime`: Instant
- `maxResponses`: Int
- `minimumResponseTime`: Int
- `tags`: List of Strings
- `totalParticipants`: Int
- `averageCompletionTime`: Double
- `accessKey`: String (private, used for decryption)

### Question

- `id`: String
- `text`: String
- `questionType`: String ("text", "radio", "checkbox", or "scale")
- `options`: Optional list of Strings (for radio and checkbox questions)
- `min`: Optional Int (for scale questions)
- `max`: Optional Int (for scale questions)

### Response

- `respondent`: Address
- `encryptedAnswers`: String

## Key Concepts

1. **Survey Creation**: When a survey is created, an access key is generated and returned to the creator. This key is required to decrypt responses later.

2. **Response Encryption**: Survey responses are encrypted on the client-side before being submitted to the metagraph.

3. **Token Rewards**: The system is prepared to distribute token rewards to survey participants, but the actual implementation depends on the specific token system used.

4. **Access Control**: Only the survey creator (with the correct access key) can decrypt and view the survey responses.

## Building and Running

To build and run the metagraph:

1. Ensure you have the Euclid Development Environment set up.
2. Run `./scripts/hydra install` to set up the project.
3. Run `./scripts/hydra build` to build the project.
4. Use `./scripts/hydra start` to start the metagraph nodes.

For more detailed instructions, refer to the Constellation Network documentation.

## Note on Security

