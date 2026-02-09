package com.jayabharathistore.app.data

import com.jayabharathistore.app.data.model.Product

object SampleData {
    
    fun getSampleProducts(): List<Product> {
        return listOf(
            // Kitchen & Cooking Essentials
            Product(
                id = "1",
                name = "MDH Lal Mirch Powder",
                description = "Premium quality red chili powder",
                price = 45.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=MDH+Lal+Mirch",
                category = "Spices & Masalas",
                inStock = true,
                stockQuantity = 50,
                unit = "100g",
                ownerId = "store_owner"
            ),
            Product(
                id = "2", 
                name = "24 Mantra Organic Turmeric Powder",
                description = "Pure organic turmeric powder",
                price = 75.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Turmeric+Powder",
                category = "Spices & Masalas",
                inStock = true,
                stockQuantity = 30,
                unit = "100g",
                ownerId = "store_owner"
            ),
            Product(
                id = "3",
                name = "Fortune Sunflower Oil",
                description = "Refined sunflower oil 1L",
                price = 120.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Sunflower+Oil",
                category = "Kitchen & Cooking",
                inStock = true,
                stockQuantity = 25,
                unit = "1L",
                ownerId = "store_owner"
            ),
            Product(
                id = "4",
                name = "Tata Salt",
                description = "Refined iodized salt",
                price = 20.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Tata+Salt",
                category = "Kitchen & Cooking",
                inStock = true,
                stockQuantity = 100,
                unit = "1kg",
                ownerId = "store_owner"
            ),
            
            // Dairy & Milk Products
            Product(
                id = "5",
                name = "Amul Taaza Fresh Milk",
                description = "Full cream fresh milk",
                price = 25.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Amul+Milk",
                category = "Dairy & Milk",
                inStock = true,
                stockQuantity = 40,
                unit = "1L",
                ownerId = "store_owner"
            ),
            Product(
                id = "6",
                name = "Amul Butter",
                description = "Salted butter 500g",
                price = 55.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Amul+Butter",
                category = "Dairy & Milk",
                inStock = true,
                stockQuantity = 20,
                unit = "500g",
                ownerId = "store_owner"
            ),
            Product(
                id = "7",
                name = "Mother Dairy Curd",
                description = "Fresh curd 400g",
                price = 30.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Fresh+Curd",
                category = "Dairy & Milk",
                inStock = true,
                stockQuantity = 35,
                unit = "400g",
                ownerId = "store_owner"
            ),
            Product(
                id = "8",
                name = "Amul Cheese Slices",
                description = "Processed cheese slices",
                price = 95.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Cheese+Slices",
                category = "Dairy & Milk",
                inStock = true,
                stockQuantity = 15,
                unit = "200g",
                ownerId = "store_owner"
            ),
            
            // Cleaning & Household
            Product(
                id = "9",
                name = "Surf Excel Matic",
                description = "Front load washing powder 1kg",
                price = 180.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Surf+Excel",
                category = "Cleaning & Household",
                inStock = true,
                stockQuantity = 25,
                unit = "1kg",
                ownerId = "store_owner"
            ),
            Product(
                id = "10",
                name = "Harpic Toilet Cleaner",
                description = "Powerful toilet cleaner 500ml",
                price = 85.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Harpic",
                category = "Cleaning & Household",
                inStock = true,
                stockQuantity = 30,
                unit = "500ml",
                ownerId = "store_owner"
            ),
            Product(
                id = "11",
                name = "Vim Dishwash Bar",
                description = "Lemon fresh dishwash bar",
                price = 20.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Vim+Bar",
                category = "Cleaning & Household",
                inStock = true,
                stockQuantity = 50,
                unit = "piece",
                ownerId = "store_owner"
            ),
            Product(
                id = "12",
                name = "Ariel Matic",
                description = "Top load washing powder 1kg",
                price = 190.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Ariel+Matic",
                category = "Cleaning & Household",
                inStock = true,
                stockQuantity = 20,
                unit = "1kg",
                ownerId = "store_owner"
            ),
            
            // Food & Groceries
            Product(
                id = "13",
                name = "Maggi Noodles",
                description = "Instant noodles 2-minute masala",
                price = 15.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Maggi+Noodles",
                category = "Food & Groceries",
                inStock = true,
                stockQuantity = 100,
                unit = "70g",
                ownerId = "store_owner"
            ),
            Product(
                id = "14",
                name = "Parle-G Biscuits",
                description = "Glucose biscuits 800g",
                price = 40.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Parle-G",
                category = "Food & Groceries",
                inStock = true,
                stockQuantity = 60,
                unit = "800g",
                ownerId = "store_owner"
            ),
            Product(
                id = "15",
                name = "Kellogg's Corn Flakes",
                description = "Original corn flakes 250g",
                price = 85.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Corn+Flakes",
                category = "Breakfast Items",
                inStock = true,
                stockQuantity = 25,
                unit = "250g",
                ownerId = "store_owner"
            ),
            Product(
                id = "16",
                name = "Red Label Tea",
                description = "Premium tea leaves 250g",
                price = 120.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Red+Label+Tea",
                category = "Snacks & Beverages",
                inStock = true,
                stockQuantity = 40,
                unit = "250g",
                ownerId = "store_owner"
            ),
            Product(
                id = "17",
                name = "Nescafe Classic Coffee",
                description = "Instant coffee powder 50g",
                price = 140.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Nescafe",
                category = "Snacks & Beverages",
                inStock = true,
                stockQuantity = 20,
                unit = "50g",
                ownerId = "store_owner"
            ),
            Product(
                id = "18",
                name = "Good Day Biscuits",
                description = "Butter cookies 100g",
                price = 25.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Good+Day",
                category = "Food & Groceries",
                inStock = true,
                stockQuantity = 80,
                unit = "100g",
                ownerId = "store_owner"
            ),
            Product(
                id = "19",
                name = "Bournvita Health Drink",
                description = "Chocolate health drink 500g",
                price = 175.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Bournvita",
                category = "Snacks & Beverages",
                inStock = true,
                stockQuantity = 30,
                unit = "500g",
                ownerId = "store_owner"
            ),
            Product(
                id = "20",
                name = "Haldiram's Namkeen",
                description = "Mixture namkeen 200g",
                price = 45.0,
                imageUrl = "https://via.placeholder.com/200x200/00C851/FFFFFF?text=Haldiram's",
                category = "Food & Groceries",
                inStock = true,
                stockQuantity = 35,
                unit = "200g",
                ownerId = "store_owner"
            )
        )
    }
    
    fun getSampleCategories(): List<String> {
        return listOf(
            "Kitchen & Cooking",
            "Spices & Masalas", 
            "Cleaning & Household",
            "Dairy & Milk",
            "Food & Groceries",
            "Snacks & Beverages",
            "Breakfast Items",
            "Personal Care",
            "Pet Care"
        )
    }
}
