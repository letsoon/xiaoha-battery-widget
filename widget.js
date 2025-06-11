const batteryNo = '8903128939'; // ← 可改成你的电池编号
const widget = new ListWidget();
widget.backgroundColor = new Color("#0088fe");

if (!batteryNo) {
    const errorText = widget.addText("⚠️ 参数缺失");
    errorText.font = Font.systemFont(10);
    errorText.textColor = Color.white();
    errorText.centerAlignText();
    Script.setWidget(widget);
    Script.complete();
    return;
}

const endpoint = `https://xiaoha.linkof.link/?batteryNo=${batteryNo}&format=json`;

async function createWidget() {
    try {
        const req = new Request(endpoint);
        const res = await req.loadJSON();
        const data = res.data;

        if(data.batteryLife >= 60){
            widget.backgroundColor = new Color("#7bf1a8");
        }else if(data.batteryLife > 20){
            widget.backgroundColor = new Color("#fef9c2");
        }else{
            widget.backgroundColor = new Color("#ff6467");
        }

        const reportDate = new Date(data.reportTime);
        const df = new DateFormatter();
        df.dateFormat = "yyyy/MM/dd HH:mm:ss"; // 可自定义格式
        const formattedTime = df.string(reportDate);

        widget.setPadding(0, 0, 0, 0);

        // 上部 spacer
        widget.addSpacer();

        // 水平居中进度圈
        const centerStack = widget.addStack();
        centerStack.addSpacer();

        const progressStack = await progressCircle(
            centerStack,
            data.batteryLife,
            "#ffffff-#ffffff",
            "rgba(255,255,255,0.3)-rgba(255,255,255,0.3)",
            90,
            8
        );

        const percentStack = progressStack.addStack();
        percentStack.centerAlignContent();

        const percentText = percentStack.addText(`${data.batteryLife}%`);
        percentText.font = Font.boldSystemFont(18);
        percentText.textColor = Color.white();
        percentText.centerAlignText();

        centerStack.addSpacer();

        // 中部 spacer
        widget.addSpacer();

        // 底部堆叠
        const bottomStack = widget.addStack();
        bottomStack.layoutHorizontally();
        bottomStack.centerAlignContent();
        bottomStack.setPadding(10, 10, 10, 10);

        // 左下 logo
        // const logoStack = bottomStack.addStack();
        // const logoBase64 = "iVBORw0KGgoAAAANSUhEUgAAABwAAAAcCAMAAABF0y+mAAAAbFBMVEUAiP4Ah/4AhP4lj/4Agv4Af/6Qv//////R4/8Aff5Uov7w9//p8/9lqf/I3v/w9f+pzv9srP/4/P+82P+Huf7e6v9xsf99tf640/8ylP6hyf9Hnf6dxv8Ag/7k7/8Aev7X5/8Adv5AmP4RjP7GGMZdAAAA9klEQVR4AdXLBwKDIAxA0QSDwb23VKX3v2MZnUfodzAewJ+Gn1noy0T0WuL3aZKSgGJWAkFImaRZltsnK4S1sqrqpOG47aToq2po2pGbdsomi7KaS7Xwmmy8J3O18pgB6za6BZx2lXUH2dvbPGxccO6fJuCq0rW2TTgPKUMxTrYCIeB5daXNSI9LxcxtSU9UULsKjxGfy2a6m3xhw8MwtOpweB/NSkdeh5vjbpHk1Z0WN76T4a7nBR0OQ9bZ8zpRXdJzySQSwxwT2NCoLpLtWaxcCKzPlPou41iCD4kQzZDlk7ZzKag794XgKxSA+jkXpBF+Q/jzHpg8EYrSfggvAAAAAElFTkSuQmCC";
        // const logoImage = Image.fromData(Data.fromBase64String(logoBase64));
        // const logo = logoStack.addImage(logoImage);
        // logo.imageSize = new Size(20, 20); // 控制 logo 尺寸
        // logoStack.addSpacer(5);

        // 右下角文本
        const infoStack = bottomStack.addStack();
        infoStack.layoutVertically();
        infoStack.centerAlignContent(); // ← 加这一句

        const idText = infoStack.addText(`电池编号：${batteryNo}`);
        idText.font = Font.systemFont(10);
        idText.textColor = Color.white();
        idText.centerAlignText();

        const timeText = infoStack.addText(`更新：${formattedTime}`);
        timeText.font = Font.systemFont(10);
        timeText.textColor = Color.white();
        timeText.centerAlignText();
        Script.setWidget(widget);
    } catch (err) {
        console.error(err);
        const errorText = widget.addText("加载失败");
        errorText.font = Font.systemFont(10);
        errorText.textColor = Color.white();
        errorText.centerAlignText();
        Script.setWidget(widget);
    }
}

await createWidget();
Script.complete();

async function progressCircle(on, value = 50, colour = "white", background = "gray", size = 56, barWidth = 5.5) {
    if (value > 1) value /= 100;
    if (value < 0) value = 0;
    if (value > 1) value = 1;

    async function isUsingDarkAppearance() {
        return !Color.dynamic(Color.white(), Color.black()).red;
    }

    let isDark = await isUsingDarkAppearance();
    if (colour.includes("-")) colour = isDark ? colour.split("-")[1] : colour.split("-")[0];
    if (background.includes("-")) background = isDark ? background.split("-")[1] : background.split("-")[0];

    let w = new WebView();
    await w.loadHTML('<canvas id="c"></canvas>');

    let base64 = await w.evaluateJavaScript(`

    let colour = "${colour}",
        background = "${background}",
        size = ${size}*3,
        lineWidth = ${barWidth}*3,
        percent = ${value * 100};

    let canvas = document.getElementById('c'),
        c = canvas.getContext('2d');
    canvas.width = size;
    canvas.height = size;
    let posX = canvas.width / 2,
        posY = canvas.height / 2,
        onePercent = 360 / 100,
        result = onePercent * percent;
    c.lineCap = 'round';
    c.beginPath();
    c.arc(posX, posY, (size-lineWidth-1)/2, (Math.PI/180) * 270, (Math.PI/180) * (270 + 360));
    c.strokeStyle = background;
    c.lineWidth = lineWidth;
    c.stroke();
    c.beginPath();
    c.strokeStyle = colour;
    c.lineWidth = lineWidth;
    c.arc(posX, posY, (size-lineWidth-1)/2, (Math.PI/180) * 270, (Math.PI/180) * (270 + result));
    c.stroke();
    completion(canvas.toDataURL().replace("data:image/png;base64,",""));
  `, true);

    const image = Image.fromData(Data.fromBase64String(base64));
    let stack = on.addStack();
    stack.size = new Size(size, size);
    stack.backgroundImage = image;
    stack.centerAlignContent();

    let padding = barWidth * 2;
    stack.setPadding(padding, padding, padding, padding);

    return stack;
}
