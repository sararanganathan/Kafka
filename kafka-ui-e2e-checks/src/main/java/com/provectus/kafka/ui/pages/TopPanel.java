package com.provectus.kafka.ui.pages;

import com.codeborne.selenide.SelenideElement;

import java.util.Arrays;
import java.util.List;

import static com.codeborne.selenide.Selenide.$x;

public class TopPanel {
    protected SelenideElement kafkaLogo = $x("//a[contains(text(),'UI for Apache Kafka')]");
    protected SelenideElement kafkaVersion = $x("//a[@title='Current commit']");
    protected SelenideElement logOutBtn = $x("//button[contains(text(),'Log out')]");
    protected SelenideElement gitBtn = $x("//a[@href='https://github.com/provectus/kafka-ui']");
    protected SelenideElement discordBtn = $x("//button[contains(text(),'Log out')]/../parent::div/a[last()]");

    public List<SelenideElement> getAllVisibleElements() {
        return Arrays.asList(kafkaLogo, kafkaVersion, logOutBtn, gitBtn, discordBtn);
    }

    public List<SelenideElement> getAllEnabledElements() {
        return Arrays.asList(logOutBtn, gitBtn, discordBtn, kafkaLogo);
    }
}
