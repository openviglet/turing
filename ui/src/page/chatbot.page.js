import React from "react";
import {
  Box,
  Button,
  ButtonPrimary,
  Label,
  Pagehead,
  TextInput,Link
} from "@primer/components";
import { PackageIcon, SearchIcon, UploadIcon } from "@primer/styled-octicons";
import TurChatbotDataService from "../services/tur-chatbot.service";
import VigJdenticon from "../commons/vig-jdenticon";

class TurChatbotPage extends React.Component {
  constructor(props) {
    super(props);
    this.retrieveChatbotAgents = this.retrieveChatbotAgents.bind(this);
    this.state = {
      chatbotAgents: [],
    };
  }

  handleMessageChanged(event) {
    this.setState({
      message: event.target.value,
    });
  }
  componentDidMount() {
    this.retrieveChatbotAgents();
  }
  retrieveChatbotAgents() {
    TurChatbotDataService.getAgents()
      .then((response) => {
        this.setState({
          chatbotAgents: response.data,
        });
        console.log(response.data);
      })
      .catch((e) => {
        console.log(e);
      });
  }
  render() {
    const { chatbotAgents } = this.state;
    return (
      <div>
        <Pagehead>
          <Box layout="grid" gridTemplateColumns="repeat(2, auto)" gridGap={3}>
            <Box>
              <TextInput
                icon={SearchIcon}
                width="50%"
                placeholder="Find a Chatbot Agent..."
              />
            </Box>
            <Box sx={{ textAlign: "right" }}>
              <Button mr={2}>
                <UploadIcon /> Import
              </Button>
              <ButtonPrimary>
                <PackageIcon /> New{" "}
              </ButtonPrimary>
            </Box>
          </Box>
        </Pagehead>
        {chatbotAgents &&
          chatbotAgents.map((chatbotAgent, index) => (
            <Pagehead key={index} paddingTop={0}>
              <VigJdenticon size="24" value={chatbotAgent.name.toLowerCase()} />
              <Link ml={1} href="#" fontSize={"large"} fontWeight={"bolder"}>
                {chatbotAgent.name}
              </Link>
              <Box marginTop={"5px"}>{chatbotAgent.description}</Box>
              <Label variant="medium" outline mt={2}>
                Enabled
              </Label>
            </Pagehead>
          ))}
      </div>
    );
  }
}

export default TurChatbotPage;
