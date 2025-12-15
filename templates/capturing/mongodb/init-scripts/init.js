// 2. Create and populate the products collection
db.products.insertMany([
    {
        id: 1,
        name: "Laptop",
        description: "High-performance ultrabook",
        price: 1250
    },
    {
        id: 2,
        name: "Smartphone",
        description: "Latest model with AMOLED display",
        price: 180
    },
    {
        id: 3,
        name: "Coffee Mug",
        description: "Ceramic mug with lid",
        price: 350
    }
]);

// 3. Create and populate the users collection
db.customers.insertMany([
    {
        id: 1,
        name: "Alice",
        surname: "Smith",
        email: "alice.smith@example.com"
    },
    {
        id: 2,
        name: "Bob",
        surname: "Johnson",
        email: "bob.johnson@example.com"
    },
    {
        id: 3,
        name: "Charlie",
        surname: "Brown",
        email: "charlie.brown@example.com"
    },
]);