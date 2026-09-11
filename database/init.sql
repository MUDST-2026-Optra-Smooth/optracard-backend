CREATE TABLE Users_Admins (
     UA_ID SERIAL PRIMARY KEY,
     UA_Username VARCHAR(255) NOT NULL,
     UA_Password VARCHAR(255) NOT NULL,
     UA_Role VARCHAR(50) NOT NULL,
     UA_Phone VARCHAR(50),
     UA_Address TEXT,
     UA_CreateDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     UA_Email VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE CardGames (
     Game_ID SERIAL PRIMARY KEY,
     Game_Name VARCHAR(255) NOT NULL,
     Game_Description TEXT,
     CONSTRAINT uq_cardgames_game_name UNIQUE (Game_Name)
);

-- A marketplace store belongs to a customer whose seller application was approved.
-- Official inventory deliberately has no store record and is identified by
-- Products.Pro_Listing_Source = 'OFFICIAL'.
CREATE TABLE Marketplace_Stores (
     Store_ID SERIAL PRIMARY KEY,
     Seller_User_ID INT NOT NULL REFERENCES Users_Admins(UA_ID),
     Store_Name VARCHAR(255) NOT NULL,
     Store_Slug VARCHAR(100) NOT NULL UNIQUE,
     Store_Status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
     Store_Description TEXT
);

CREATE TABLE Products (
     Pro_ID SERIAL PRIMARY KEY,
     Game_ID INT REFERENCES CardGames(Game_ID),
     Pro_Name VARCHAR(255) NOT NULL,
     Pro_SKU VARCHAR(80) UNIQUE,
     Pro_Cost DECIMAL(10, 2) NOT NULL,
     Pro_PriceOfSell DECIMAL(10, 2) NOT NULL,
     Pro_Quantity INT NOT NULL DEFAULT 0,
     Pro_Type VARCHAR(100),
     Pro_ImageURL TEXT,
     Pro_Attributes TEXT,
     Pro_Description TEXT,
     Is_Active BOOLEAN DEFAULT TRUE,
     Pro_Listing_Source VARCHAR(20) NOT NULL DEFAULT 'OFFICIAL'
         CHECK (Pro_Listing_Source IN ('OFFICIAL', 'MARKETPLACE')),
     Store_ID INT REFERENCES Marketplace_Stores(Store_ID) ON DELETE SET NULL,
     CHECK (
         (Pro_Listing_Source = 'OFFICIAL' AND Store_ID IS NULL)
         OR (Pro_Listing_Source = 'MARKETPLACE' AND Store_ID IS NOT NULL)
     )
);

CREATE TABLE Orders (
     Ord_ID SERIAL PRIMARY KEY,
     UA_ID INT REFERENCES Users_Admins(UA_ID),
     Ord_Total_Price DECIMAL(10, 2) NOT NULL,
     Ord_PayMethod VARCHAR(100),
     Ord_Status VARCHAR(50),
     Ord_ShippingAddress TEXT,
     Ord_TrackingNumber VARCHAR(100),
     Ord_CreateDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Carts (
     Cart_ID SERIAL PRIMARY KEY,
     UA_ID INT REFERENCES Users_Admins(UA_ID),
     Created_At TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     Updated_At TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE CartItems (
     CartItems_ID SERIAL PRIMARY KEY,
     Cart_ID INT REFERENCES Carts(Cart_ID) ON DELETE CASCADE,
     Pro_ID INT REFERENCES Products(Pro_ID) ON DELETE CASCADE,
     CartItems_Quantity INT NOT NULL DEFAULT 1
);

CREATE TABLE OrderItems (
     OrdItems_ID SERIAL PRIMARY KEY,
     Ord_ID INT REFERENCES Orders(Ord_ID) ON DELETE CASCADE,
     Pro_ID INT REFERENCES Products(Pro_ID),
     OrdItems_Quantity INT NOT NULL,
     OrdItems_Price DECIMAL(10, 2) NOT NULL
);

